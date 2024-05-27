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
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.UpdateTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.helper.TaskScheduleCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateTaskScheduleBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val updateTaskScheduleCommandHandler: UpdateTaskScheduleCommandHandler,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val taskScheduleCommandHandlerHelper: TaskScheduleCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize(usedByController = true)
  open fun handle(commands: List<UpdateTaskScheduleCommand>) {
    if (commands.isEmpty()) {
      return
    }

    val taskSchedules =
        taskScheduleRepository.findWithDetailsByTaskIdentifierIn(commands.map { it.taskIdentifier })

    taskScheduleCommandHandlerHelper.assertTaskSchedulesFromSameProject(taskSchedules)

    businessTransactionManager.doBatchInBusinessTransaction(taskSchedules.projectIdentifier()) {
      commands.map { updateTaskScheduleCommandHandler.handle(command = it) }
    }
  }

  private fun List<TaskSchedule>.projectIdentifier() =
      this.stream().findAny().orElseThrow().project.identifier
}
