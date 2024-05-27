/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_CRAFT_IN_USE
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.DeleteProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.service.ProjectCraftListService
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshotStore
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteProjectCraftCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: ProjectCraftSnapshotStore,
    private val projectCraftListService: ProjectCraftListService,
    private val taskRepository: TaskRepository,
    private val milestoneRepository: MilestoneRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectCraftAuthorizationComponent.hasDeletePermissionOnProjectCraft(#command.identifier)")
  open fun handle(command: DeleteProjectCraftCommand) {
    checkProjectCraftIsNotInUse(command.identifier)

    projectCraftListService.removeProjectCraftFromList(
        projectIdentifier = command.projectIdentifier, projectCraftIdentifier = command.identifier)

    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .emitEvent(DELETED)
        .to(eventBus)
  }

  private fun checkProjectCraftIsNotInUse(identifier: ProjectCraftId) {
    if (taskRepository.existsByProjectCraftIdentifier(identifier) ||
        milestoneRepository.existsByCraftIdentifier(identifier)) {
      throw PreconditionViolationException(PROJECT_CRAFT_VALIDATION_ERROR_CRAFT_IN_USE)
    }
  }
}
