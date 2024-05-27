/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.service.WorkAreaListService
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateWorkAreaCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val workAreaListService: WorkAreaListService,
    private val workAreaListRepository: WorkAreaListRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val projectRepository: ProjectRepository
) {

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#command.projectRef)")
  open fun handle(command: CreateWorkAreaCommand): WorkAreaId {

    val workAreaList = findWorkAreaList(command.projectRef)
    existsWorkAreaByName(command.name, command.projectRef, command.parentRef)

    val totalWorkAreas = workAreaList.workAreas.size
    val workAreaPosition: Int = validateAndGetWorkAreaPosition(command.position, totalWorkAreas)

    val workAreaSnapshot =
        WorkAreaSnapshot(
                identifier = command.identifier,
                version = INITIAL_SNAPSHOT_VERSION,
                name = command.name,
                projectRef = command.projectRef,
                parentRef = command.parentRef)
            .toCommandHandler()
            .emitEvent(CREATED)
            .to(eventBus)
            .andReturnSnapshot()

    workAreaListService.addWorkAreaToList(
        workAreaSnapshot, workAreaList.identifier, workAreaPosition, command.workAreaListVersion)

    return workAreaSnapshot.identifier
  }

  private fun findWorkAreaList(projectIdentifier: ProjectId) =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: throw PreconditionViolationException(WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND)

  private fun existsWorkAreaByName(name: String, projectRef: ProjectId, parentRef: WorkAreaId?) {
    val projectId = findProjectIdOrFail(projectRef)
    if (workAreaRepository.existsByNameIgnoreCaseAndProjectIdAndParent(name, projectId, parentRef))
        throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_USED_NAME)
  }

  private fun findProjectIdOrFail(projectId: ProjectId): Long =
      requireNotNull(projectRepository.findIdByIdentifier(projectId)) {
        "Could not find Project $projectId"
      }

  private fun validateAndGetWorkAreaPosition(position: Int?, totalWorkAreas: Int) =
      when {
        position == null -> totalWorkAreas
        position < 1 || position >= totalWorkAreas + 2 ->
            throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION)
        else -> position - 1
      }
}
