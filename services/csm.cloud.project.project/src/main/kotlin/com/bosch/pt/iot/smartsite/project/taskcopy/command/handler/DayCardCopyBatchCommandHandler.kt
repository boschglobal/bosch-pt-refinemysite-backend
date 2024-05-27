/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.command.handler

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.CreateDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.DayCardCopyCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.AddDayCardsToTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard.AddDayCardToTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DayCardCopyBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val createDayCardBatchCommandHandler: CreateDayCardBatchCommandHandler,
    private val addDayCardToTaskScheduleCommandHandler: AddDayCardToTaskScheduleCommandHandler,
    private val taskScheduleRepository: TaskScheduleRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskCopyAuthorizationComponent.hasCopyTaskPermissionOnProject(#projectIdentifier)")
  open fun handle(commands: List<DayCardCopyCommand>, projectIdentifier: ProjectId) {
    if (commands.isEmpty()) {
      return
    }

    val dayCardCopyCommandByIdentifier = commands.associateBy { it.copyFromIdentifier }

    val slotsWithDayCardByScheduleIdentifier =
        taskScheduleRepository
            .findAllDayCardsByDayCardIdentifierIn(dayCardCopyCommandByIdentifier.keys)
            .groupBy { it.identifier }

    val copyToScheduleByIdentifier =
        taskScheduleRepository
            .findTaskScheduleWithoutDayCardsDtosByIdentifiers(
                commands.map { it.copyToTaskScheduleIdentifier })
            .associateBy { it.identifier }

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      // For each list of day cards of a schedule:
      // - create CreateDayCardCommands for each of the day cards
      // - add created day cards to the new task schedule at the correct date
      slotsWithDayCardByScheduleIdentifier.forEach { scheduleWithSlots ->
        val createDayCardCommands = mutableListOf<CreateDayCardCommand>()
        val addDayCardToTaskScheduleCommands = mutableListOf<AddDayCardsToTaskScheduleCommand>()

        // For each day card of the existing schedule:
        // - builds the new copied day card command
        // - builds the add day card command to the new copied schedule
        scheduleWithSlots.value.mapIndexed { index, slot ->
          val copyCommand =
              requireNotNull(dayCardCopyCommandByIdentifier[slot.slotsDayCardIdentifier])
          val destinationTaskSchedule =
              requireNotNull(copyToScheduleByIdentifier[copyCommand.copyToTaskScheduleIdentifier])

          createDayCardCommands.add(
              CreateDayCardCommand(
                  identifier = copyCommand.identifier,
                  taskIdentifier = destinationTaskSchedule.taskIdentifier,
                  title = slot.slotsDayCardTitle,
                  manpower = slot.slotsDayCardManpower,
                  notes = slot.slotsDayCardNotes,
                  status = OPEN,
                  reason = null))

          addDayCardToTaskScheduleCommands.add(
              AddDayCardsToTaskScheduleCommand(
                  taskScheduleIdentifier = copyCommand.copyToTaskScheduleIdentifier,
                  projectIdentifier = projectIdentifier,
                  taskIdentifier = destinationTaskSchedule.taskIdentifier,
                  date = slot.slotsDate.plusDays(copyCommand.shiftDays),
                  dayCardIdentifier = copyCommand.identifier,
                  eTag = ETag.from((destinationTaskSchedule.version + index).toString())))
        }

        // At the moment, the add of the day cards and update of the task schedule slots are still
        // separated in the controller, therefor we need to replicate the same behaviour here
        createDayCardBatchCommandHandler.handle(createDayCardCommands, projectIdentifier)

        addDayCardToTaskScheduleCommands.forEach {
          addDayCardToTaskScheduleCommandHandler.handle(it)
        }
      }
    }
  }
}
