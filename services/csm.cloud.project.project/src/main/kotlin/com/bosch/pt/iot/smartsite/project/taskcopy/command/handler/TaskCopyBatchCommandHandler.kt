/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.command.handler

import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.batch.CreateTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.TaskCopyCommand
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.TaskScheduleCopyCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TaskCopyBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createTaskBatchCommandHandler: CreateTaskBatchCommandHandler,
    private val taskScheduleCopyBatchCommandHandler: TaskScheduleCopyBatchCommandHandler,
    private val taskRepository: TaskRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskCopyAuthorizationComponent.hasCopyTaskPermissionOnProject(#projectIdentifier)")
  open fun handle(commands: List<TaskCopyCommand>, projectIdentifier: ProjectId): List<TaskId> {
    if (commands.isEmpty()) {
      return emptyList()
    }

    // Find all tasks to be copied and respective task schedules
    val tasksToCopy =
        taskRepository
            .findAllWithDetailsByIdentifierInAndProjectIdentifier(
                commands.map { it.copyFromIdentifier }.toSet(), projectIdentifier)
            .associateBy { it.identifier }

    // Validate that every task exists and belongs to given project
    require(commands.map { it.copyFromIdentifier }.distinct().size == tasksToCopy.size) {
      "The task identifiers to copy were not found or are not from the given project"
    }

    return businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      commands
          .map { it.toCreateTaskCommand(requireNotNull(tasksToCopy[it.copyFromIdentifier])) }
          .let { createTaskBatchCommandHandler.handle(it) }

      commands
          .mapNotNull {
            requireNotNull(tasksToCopy[it.copyFromIdentifier]).taskSchedule?.let { schedule ->
              it.toTaskScheduleCopyCommand(schedule.identifier)
            }
          }
          .let { taskScheduleCopyBatchCommandHandler.handle(it, projectIdentifier) }

      commands.map { it.identifier }
    }
  }

  private fun TaskCopyCommand.toCreateTaskCommand(task: Task) =
      CreateTaskCommand(
          identifier = identifier,
          projectIdentifier = task.project.identifier,
          name = task.name,
          description = task.description,
          location = task.location,
          projectCraftIdentifier = task.projectCraft.identifier,
          assigneeIdentifier = null,
          workAreaIdentifier = getWorkAreaOrNull(task.workArea?.identifier),
          status = DRAFT)

  private fun TaskCopyCommand.toTaskScheduleCopyCommand(taskScheduleIdentifier: TaskScheduleId) =
      TaskScheduleCopyCommand(
          copyFromIdentifier = taskScheduleIdentifier,
          copyToTaskIdentifier = identifier,
          identifier = TaskScheduleId(),
          shiftDays = shiftDays,
          includeDayCards = includeDayCards)

  private fun TaskCopyCommand.getWorkAreaOrNull(
      currentWorkAreaIdentifier: WorkAreaId?
  ): WorkAreaId? {
    return if (parametersOverride?.workAreaId != null) {
      if (parametersOverride.workAreaId.isEmpty) {
        null
      } else {
        parametersOverride.workAreaId.identifier
      }
    } else {
      currentWorkAreaIdentifier
    }
  }
}
