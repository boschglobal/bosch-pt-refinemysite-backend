/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.UpdateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshotStore
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateProjectCraftCommandHandler(
    private val snapshotStore: ProjectCraftSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val repository: ProjectCraftRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectCraftAuthorizationComponent.hasUpdatePermissionOnProjectCraft(#command.identifier)")
  open fun handle(command: UpdateProjectCraftCommand): ProjectCraftId =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .checkPrecondition { checkProjectCraftNameIsUnique(it, command) }
          .onFailureThrow(PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME)
          .assertVersionMatches(command.version)
          .update { it.copy(name = command.name, color = command.color) }
          .emitEvent(UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun checkProjectCraftNameIsUnique(
      snapshot: ProjectCraftSnapshot,
      command: UpdateProjectCraftCommand
  ): Boolean {
    val isRenamed = !command.name.equals(snapshot.name, ignoreCase = true)

    return !(isRenamed &&
        repository.existsByNameIgnoreCaseAndProjectIdentifier(
            command.name, snapshot.projectIdentifier))
  }
}
