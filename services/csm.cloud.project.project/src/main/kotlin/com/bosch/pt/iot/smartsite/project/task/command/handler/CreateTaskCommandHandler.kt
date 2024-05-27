/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.helper.TaskCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.assertCreateTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskCreateService
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateTaskCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val taskCreateService: TaskCreateService,
    private val taskCommandHandlerHelper: TaskCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectAuthorizationComponent.hasCreateAndAssignTaskPermissionOnProject(" +
          "#command.projectIdentifier, #command.assigneeIdentifier)")
  open fun handle(command: CreateTaskCommand): TaskId {
    assertCreateTaskPossible(command.status, command.assigneeIdentifier)

    val identifier =
        TaskSnapshot(
                identifier = command.identifier,
                version = INITIAL_SNAPSHOT_VERSION,
                projectIdentifier =
                    taskCommandHandlerHelper.returnProjectIfExistsAndRequired(
                        command.projectIdentifier),
                name = command.name,
                description = command.description,
                location = command.location,
                projectCraftIdentifier =
                    taskCommandHandlerHelper.returnProjectCraftIfExists(
                        command.projectCraftIdentifier, command.projectIdentifier),
                assigneeIdentifier = null,
                workAreaIdentifier =
                    taskCommandHandlerHelper.returnWorkAreaIfExistsAndRequired(
                        command.workAreaIdentifier, command.projectIdentifier),
                status = DRAFT)
            .toCommandHandler()
            .emitEvent(CREATED)
            .to(eventBus)
            .andReturnSnapshot()
            .identifier

    taskCreateService.changeAssignAndStatusIfRequired(
        identifier, command.assigneeIdentifier, command.status)

    return identifier
  }
}
