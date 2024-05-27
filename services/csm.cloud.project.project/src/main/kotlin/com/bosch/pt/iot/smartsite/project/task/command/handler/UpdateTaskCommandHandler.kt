/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.helper.TaskCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.assertUpdateTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskUpdateService
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskUpdateService: TaskUpdateService,
    private val taskCommandHandlerHelper: TaskCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskAuthorizationComponent.hasEditAndAssignPermissionOnTask(" +
          "#command.identifier, #command.assigneeIdentifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey("identifier") command: UpdateTaskCommand) {
    val snapshot = snapshotStore.findOrFail(command.identifier)

    assertUpdateTaskPossible(snapshot.status, command.status, command.assigneeIdentifier)

    snapshot
        .toCommandHandler()
        .assertVersionMatches(command.version!!)
        .update {
          it.copy(
              name = command.name,
              description = command.description,
              location = command.location,
              projectCraftIdentifier =
                  taskCommandHandlerHelper.returnProjectCraftIfExists(
                      command.projectCraftIdentifier, command.projectIdentifier),
              workAreaIdentifier =
                  taskCommandHandlerHelper.returnWorkAreaIfExistsAndRequired(
                      command.workAreaIdentifier, command.projectIdentifier))
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)

    taskUpdateService.changeAssignAndStatusIfRequired(
        command.identifier,
        snapshot.assigneeIdentifier,
        snapshot.status,
        command.assigneeIdentifier,
        command.status)
  }
}
