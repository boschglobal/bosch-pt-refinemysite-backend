/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler.list

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.ReorderWorkAreaListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshotStore
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshotStore
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ReorderWorkAreaListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: WorkAreaListSnapshotStore,
    private val workAreaSnapshotStore: WorkAreaSnapshotStore,
    private val workAreaListRepository: WorkAreaListRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@workAreaAuthorizationComponent.hasUpdatePermissionOnWorkArea(#command.workAreaRef)")
  open fun handle(command: ReorderWorkAreaListCommand) {
    val workAreaSnapshot = workAreaSnapshotStore.findOrFail(command.workAreaRef)
    val workAreaList = findWorkAreaList(workAreaSnapshot.projectRef)

    val workAreaListSnapshot = snapshotStore.findOrFail(workAreaList.identifier)

    if (command.position < 1 || command.position > workAreaListSnapshot.workAreaRefs.size) {
      throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION)
    }

    if (workAreaListSnapshot.shouldReorderWorkArea(command.workAreaRef, command.position)) {
      workAreaListSnapshot
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .update {
            it.copy(
                workAreaRefs =
                    it.workAreaRefs.also { workAreaList ->
                      workAreaList.remove(command.workAreaRef)
                      workAreaList.add(command.position - 1, command.workAreaRef)
                    })
          }
          .emitEvent(REORDERED)
          .ifSnapshotWasChanged()
          .to(eventBus)
    }
  }

  private fun WorkAreaListSnapshot.shouldReorderWorkArea(workAreaRef: WorkAreaId, position: Int) =
      workAreaRefs[position - 1] != workAreaRef

  private fun findWorkAreaList(projectIdentifier: ProjectId) =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: throw PreconditionViolationException(Key.WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND)
}
