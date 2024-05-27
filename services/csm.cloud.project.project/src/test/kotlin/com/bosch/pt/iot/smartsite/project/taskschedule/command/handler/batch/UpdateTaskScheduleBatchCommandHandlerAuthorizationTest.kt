/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleBatchDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.AbstractTaskScheduleAuthorizationTest
import java.time.LocalDate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateTaskScheduleBatchCommandHandlerAuthorizationTest :
    AbstractTaskScheduleAuthorizationTest() {

  @Autowired
  private lateinit var updateTaskScheduleBatchCommandHandler: UpdateTaskScheduleBatchCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `edit multiple permission is granted to`(userType: UserTypeAccess) {
    val taskSchedules: MutableCollection<SaveTaskScheduleBatchDto> = ArrayList()
    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            taskSchedule.identifier,
            0L,
            taskWithScheduleIdentifier,
            LocalDate.now(),
            LocalDate.now().plusDays(10),
            emptyMap()))
    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            taskSchedule2.identifier,
            0L,
            taskWithScheduleIdentifier2,
            LocalDate.now(),
            LocalDate.now().plusDays(10),
            emptyMap()))

    checkAccessWith(userType) {
      updateTaskScheduleBatchCommandHandler.handle(
          taskSchedules.map {
            UpdateTaskScheduleCommand(
                identifier = it.identifier!!,
                taskIdentifier = it.taskIdentifier,
                start = it.start,
                version = it.version,
                end = it.end,
                slots = it.slots)
          })
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `edit multiple permission is granted to task with slots`(userType: UserTypeAccess) {
    val taskSchedules: MutableCollection<SaveTaskScheduleBatchDto> = ArrayList()

    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            taskSchedule.identifier,
            0L,
            taskWithScheduleIdentifier,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(11),
            taskSchedule.slots!!
                .map { it.dayCard }
                .associate { it.identifier to LocalDate.now().plusDays(2) }))

    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            taskSchedule2.identifier,
            0L,
            taskWithScheduleIdentifier2,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(21),
            taskSchedule2.slots!!
                .map { it.dayCard }
                .associate { it.identifier to LocalDate.now().plusDays(20) }))

    checkAccessWith(userType) {
      updateTaskScheduleBatchCommandHandler.handle(
          taskSchedules.map {
            UpdateTaskScheduleCommand(
                identifier = it.identifier!!,
                taskIdentifier = it.taskIdentifier,
                version = it.version,
                start = it.start,
                end = it.end,
                slots = it.slots)
          })
    }
  }
}
