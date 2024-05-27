/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.AddProjectCraftToListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshotStore
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList.Companion.MAX_CRAFTS_ALLOWED
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AddProjectCraftToListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: ProjectCraftListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: AddProjectCraftToListCommand) {
    val position = calculateProjectCraftPosition(command.position, command.projectCraftListSize)

    snapshotStore
        .findOrFail(command.projectCraftListIdentifier)
        .toCommandHandler()
        .checkPrecondition {
          validateNotContainProjectCraft(command.identifier, it.projectCraftIdentifiers)
        }
        .onFailureThrow {
          error(
              "Project craft ${command.identifier} is already contained in project craft list " +
                  "${command.projectCraftListIdentifier}")
        }
        .checkPrecondition { validateProjectCraftListIsNotFull(command.projectCraftListSize) }
        .onFailureThrow(PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)
        .assertVersionMatches(command.projectCraftListVersion)
        .update {
          it.copy(
              projectCraftIdentifiers =
                  it.projectCraftIdentifiers.also { projectCraftIdentifiers ->
                    projectCraftIdentifiers.add(position, command.identifier)
                  })
        }
        .emitEvent(ITEMADDED)
        .ifSnapshotWasChanged()
        .to(eventBus)
  }

  private fun calculateProjectCraftPosition(position: Int?, totalProjectCrafts: Int) =
      when {
        position == null -> totalProjectCrafts
        position < 1 || position > totalProjectCrafts + 1 ->
            throw PreconditionViolationException(
                PROJECT_CRAFT_LIST_VALIDATION_ERROR_INVALID_POSITION)
        else -> position - 1
      }

  private fun validateNotContainProjectCraft(
      projectCraftIdentifier: ProjectCraftId,
      projectCraftListIdentifiers: MutableList<ProjectCraftId>
  ) = !projectCraftListIdentifiers.contains(projectCraftIdentifier)

  private fun validateProjectCraftListIsNotFull(totalProjectCrafts: Int) =
      totalProjectCrafts < MAX_CRAFTS_ALLOWED
}
