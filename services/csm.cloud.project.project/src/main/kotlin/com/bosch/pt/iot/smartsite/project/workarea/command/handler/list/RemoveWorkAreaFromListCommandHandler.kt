/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.workarea.command.api.RemoveWorkAreaFromListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshotStore
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class RemoveWorkAreaFromListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: WorkAreaListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: RemoveWorkAreaFromListCommand) =
      snapshotStore
          .findOrFail(command.workAreaListRef)
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .update {
            it.copy(
                workAreaRefs =
                    it.workAreaRefs.also { workAreaList ->
                      workAreaList.remove(command.identifier)
                    })
          }
          .emitEvent(ITEMADDED)
          .to(eventBus)
}
