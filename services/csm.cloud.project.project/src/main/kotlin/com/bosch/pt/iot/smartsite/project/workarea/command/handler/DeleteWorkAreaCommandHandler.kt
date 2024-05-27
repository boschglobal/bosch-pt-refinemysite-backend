/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_WORK_AREA_IN_USE
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_WORK_AREA_IS_PARENT
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.DeleteWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.service.WorkAreaListService
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshotStore
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.apache.commons.collections4.CollectionUtils.isNotEmpty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteWorkAreaCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: WorkAreaSnapshotStore,
    private val workAreaListService: WorkAreaListService,
    private val workAreaListRepository: WorkAreaListRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val milestoneRepository: MilestoneRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@workAreaAuthorizationComponent.hasDeletePermissionOnWorkArea(#command.identifier)")
  open fun handle(command: DeleteWorkAreaCommand) {
    val workAreaSnapshot = snapshotStore.findOrFail(command.identifier)
    existsWorkAreaList(workAreaSnapshot.projectRef)
    val workArea = findWorkArea(workAreaSnapshot.identifier, workAreaSnapshot.projectRef)

    isWorkAreaInUse(workArea)
    isNotParent(workArea.identifier, workAreaSnapshot.projectRef)

    // remove workArea from list
    workAreaListService.removeWorkAreaFromList(workAreaSnapshot)

    workAreaSnapshot
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .emitEvent(DELETED)
        .to(eventBus)
  }

  private fun findWorkArea(identifier: WorkAreaId, projectIdentifier: ProjectId) =
      workAreaRepository.findWorkAreaByIdentifierAndProjectIdentifier(identifier, projectIdentifier)
          ?: throw PreconditionViolationException(WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND)

  private fun existsWorkAreaList(projectIdentifier: ProjectId) {
    if (!workAreaListRepository.existsByProjectIdentifier(projectIdentifier)) {
      throw PreconditionViolationException(WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND)
    }
  }

  private fun isNotParent(parentRef: WorkAreaId, projectIdentifier: ProjectId) {
    if (workAreaRepository.countByParentAndProjectIdentifier(parentRef, projectIdentifier) > 0) {
      throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_WORK_AREA_IS_PARENT)
    }
  }

  private fun isWorkAreaInUse(workArea: WorkArea) {
    if (isNotEmpty(workArea.tasks) ||
        milestoneRepository.existsByWorkAreaIdentifier(workArea.identifier)) {
      throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_WORK_AREA_IN_USE)
    }
  }
}
