/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler.list

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.AddWorkAreaToListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshotStore
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AddWorkAreaToListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: WorkAreaListSnapshotStore,
    private val workAreaListRepository: WorkAreaListRepository
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: AddWorkAreaToListCommand): WorkAreaListId {
    findWorkAreaList(command.projectRef)

    return snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .update {
          it.copy(
              workAreaRefs =
                  it.workAreaRefs.also { workAreaRefs ->
                    workAreaRefs.add(
                        adjustPositionIfRequired(workAreaRefs, command.position),
                        command.workAreaRef)
                  })
        }
        .emitEvent(ITEMADDED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun adjustPositionIfRequired(workAreaList: List<WorkAreaId>, position: Int) =
      when {
        position < 0 -> 0
        position >= workAreaList.size -> workAreaList.size
        else -> position
      }

  private fun findWorkAreaList(projectIdentifier: ProjectId) =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: throw PreconditionViolationException(Key.WORK_AREA_LIST_VALIDATION_ERROR_NOT_FOUND)
}
