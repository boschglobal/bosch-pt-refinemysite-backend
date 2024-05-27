/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.command.handler

import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.DayCardCopyCommand
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.TaskScheduleCopyCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.CreateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TaskScheduleCopyBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createTaskScheduleBatchCommandHandler: CreateTaskScheduleBatchCommandHandler,
    private val dayCardCopyBatchCommandHandler: DayCardCopyBatchCommandHandler,
    private val taskScheduleRepository: TaskScheduleRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskCopyAuthorizationComponent.hasCopyTaskPermissionOnProject(#projectIdentifier)")
  open fun handle(commands: List<TaskScheduleCopyCommand>, projectIdentifier: ProjectId) {
    if (commands.isEmpty()) {
      return
    }

    val scheduleIdentifiers = commands.map { it.copyFromIdentifier }.toSet()
    val scheduleIdentifiersForDayCardCopy =
        commands.filter { it.includeDayCards }.map { it.copyFromIdentifier }.toSet()

    val schedulesByIdentifier =
        taskScheduleRepository
            .findTaskScheduleWithoutDayCardsDtosByIdentifiers(scheduleIdentifiers)
            .associateBy { it.identifier }

    val schedulesSlotsToCopyByIdentifier =
        taskScheduleRepository
            .findAllDayCardsByIdentifierIn(scheduleIdentifiersForDayCardCopy)
            .groupBy { it.identifier }

    // Check if all expected schedules are found and from the expected project
    require(scheduleIdentifiers.size == schedulesByIdentifier.size) {
      "The task schedule identifiers to copy were not found"
    }
    require(
        schedulesByIdentifier.values
            .filter { it.taskProjectIdentifier == projectIdentifier }
            .size == schedulesByIdentifier.size) {
          "The task schedule identifiers to copy are not from the given project"
        }

    val createTaskScheduleCommands = mutableListOf<CreateTaskScheduleCommand>()
    val dayCardCopyCommands = mutableListOf<DayCardCopyCommand>()

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      // Create CreateTaskScheduleCommands for each task schedule to copy
      commands.map {
        val scheduleToCopy = requireNotNull(schedulesByIdentifier[it.copyFromIdentifier])

        createTaskScheduleCommands.add(
            CreateTaskScheduleCommand(
                identifier = it.identifier,
                taskIdentifier = it.copyToTaskIdentifier,
                start = scheduleToCopy.start?.plusDays(it.shiftDays),
                end = scheduleToCopy.end?.plusDays(it.shiftDays),
                slots = mapOf()))

        if (it.includeDayCards) {
          schedulesSlotsToCopyByIdentifier[it.copyFromIdentifier]?.forEach { slot ->
            dayCardCopyCommands.add(it.toDayCardCopyCommand(slot.slotsDayCardIdentifier))
          }
        }
      }

      createTaskScheduleBatchCommandHandler.handle(createTaskScheduleCommands)
      dayCardCopyBatchCommandHandler.handle(dayCardCopyCommands, projectIdentifier)
    }
  }

  private fun TaskScheduleCopyCommand.toDayCardCopyCommand(dayCardIdentifier: DayCardId) =
      DayCardCopyCommand(
          copyFromIdentifier = dayCardIdentifier,
          copyToTaskScheduleIdentifier = identifier,
          shiftDays = shiftDays)
}
