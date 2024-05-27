/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.ReorderProjectCraftListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshotStore
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ReorderProjectCraftListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: ProjectCraftListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: ReorderProjectCraftListCommand) =
      snapshotStore
          .findOrFail(command.projectCraftListIdentifier)
          .toCommandHandler()
          .checkPrecondition {
            validateProjectCraftPosition(command.position, it.projectCraftIdentifiers.size)
          }
          .onFailureThrow(PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)
          .assertVersionMatches(command.projectCraftListVersion)
          .update {
            it.copy(
                projectCraftIdentifiers =
                    it.projectCraftIdentifiers.also { projectCraftIdentifiers ->
                      projectCraftIdentifiers.remove(command.identifier)
                      projectCraftIdentifiers.add(command.position - 1, command.identifier)
                    })
          }
          .emitEvent(REORDERED)
          .ifSnapshotWasChanged()
          .to(eventBus)

  private fun validateProjectCraftPosition(position: Int, totalProjectCrafts: Int) =
      position in 1..totalProjectCrafts
}
