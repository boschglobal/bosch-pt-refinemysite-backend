/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.CreateTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateTaskScheduleBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createTaskScheduleCommandHandler: CreateTaskScheduleCommandHandler,
    private val taskRepository: TaskRepository,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(commands: List<CreateTaskScheduleCommand>) {
    if (commands.isEmpty()) {
      return
    }

    val taskIdentifiers = commands.associateBy { it.taskIdentifier }
    val tasks = taskRepository.findAllByIdentifierIn(taskIdentifiers.keys)

    taskScheduleCommandHandlerHelper.assertTasksFromSameProject(tasks)

    businessTransactionManager.doBatchInBusinessTransaction(tasks.projectIdentifier()) {
      commands.map { createTaskScheduleCommandHandler.handle(it) }
    }
  }

  private fun Collection<Task>.projectIdentifier() =
      this.stream().findAny().orElseThrow().project.identifier
}
