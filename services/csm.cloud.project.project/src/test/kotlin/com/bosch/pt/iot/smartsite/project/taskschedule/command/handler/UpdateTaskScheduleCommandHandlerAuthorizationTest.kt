/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import java.time.LocalDate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateTaskScheduleCommandHandlerAuthorizationTest : AbstractTaskScheduleAuthorizationTest() {

  @Autowired private lateinit var updateTaskScheduleCommandHandler: UpdateTaskScheduleCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `edit permission is granted to`(userType: UserTypeAccess) {
    eventStreamGenerator
        .submitTask("someTask") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantCreator")
        }
        .submitTaskSchedule("someTaskSchedule")

    val taskSchedule =
        repositories.findTaskScheduleWithDetails(
            EventStreamGeneratorStaticExtensions.getIdentifier("someTaskSchedule")
                .asTaskScheduleId())!!

    val taskScheduleDto =
        SaveTaskScheduleDto(LocalDate.now(), LocalDate.now().plusDays(10), emptyMap())

    checkAccessWith(userType) {
      updateTaskScheduleCommandHandler.handle(
          UpdateTaskScheduleCommand(
              identifier = taskSchedule.identifier,
              taskIdentifier = taskWithScheduleIdentifier,
              start = taskScheduleDto.start,
              end = taskScheduleDto.end,
              version = ETag.from("0").toVersion(),
              slots = taskScheduleDto.slots))
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `edit permission is granted to task with slots`(userType: UserTypeAccess) {
    eventStreamGenerator
        .submitTask("someTask") {
          it.status = TaskStatusEnumAvro.OPEN
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantCreator")
        }
        .submitTaskSchedule("someTaskSchedule")
        .submitDayCardG2("someDayCard")
        .submitTaskSchedule("someTaskSchedule") {
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(
                      LocalDate.now().toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("someDayCard")))
        }

    val taskIdentifier = EventStreamGeneratorStaticExtensions.getIdentifier("someTask").asTaskId()
    val taskSchedule =
        repositories.findTaskScheduleWithDetails(
            EventStreamGeneratorStaticExtensions.getIdentifier("someTaskSchedule")
                .asTaskScheduleId())!!

    val taskScheduleDto =
        SaveTaskScheduleDto(
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(11),
            taskSchedule.slots!!
                .map { it.dayCard }
                .associate { it.identifier to LocalDate.now().plusDays(2) })

    checkAccessWith(userType) {
      updateTaskScheduleCommandHandler.handle(
          UpdateTaskScheduleCommand(
              identifier = taskSchedule.identifier,
              taskIdentifier = taskIdentifier,
              version = ETag.from("0").toVersion(),
              start = taskScheduleDto.start,
              end = taskScheduleDto.end,
              slots = taskScheduleDto.slots),
      )
    }
  }
}
