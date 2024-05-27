/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.RemoveProjectCraftFromListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshotStore
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class RemoveProjectCraftFromListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: ProjectCraftListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: RemoveProjectCraftFromListCommand) =
      snapshotStore
          .findOrFail(command.projectCraftListIdentifier)
          .toCommandHandler()
          .checkPrecondition {
            validateContainProjectCraft(command.identifier, it.projectCraftIdentifiers)
          }
          .onFailureThrow {
            error(
                "Project craft ${command.identifier} is not contained in project craft list " +
                    "${command.projectCraftListIdentifier}")
          }
          .update {
            it.copy(
                projectCraftIdentifiers =
                    it.projectCraftIdentifiers.also { projectCraftList ->
                      projectCraftList.remove(command.identifier)
                    })
          }
          .emitEvent(ITEMREMOVED)
          .ifSnapshotWasChanged()
          .to(eventBus)

  private fun validateContainProjectCraft(
      projectCraftIdentifier: ProjectCraftId,
      projectCraftListIdentifiers: MutableList<ProjectCraftId>
  ) = projectCraftListIdentifiers.contains(projectCraftIdentifier)
}
