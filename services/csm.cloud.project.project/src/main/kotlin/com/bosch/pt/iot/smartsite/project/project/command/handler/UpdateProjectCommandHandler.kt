/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.command.api.UpdateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshotStore
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateProjectCommandHandler(
    private val snapshotStore: ProjectSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasUpdatePermissionOnProject(#command.identifier)")
  open fun handle(command: UpdateProjectCommand) {
    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .update {
          it.copy(
              client = command.client,
              description = command.description,
              start = command.start,
              end = command.end,
              projectNumber = command.projectNumber,
              title = command.title,
              category = command.category,
              address = command.address)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
