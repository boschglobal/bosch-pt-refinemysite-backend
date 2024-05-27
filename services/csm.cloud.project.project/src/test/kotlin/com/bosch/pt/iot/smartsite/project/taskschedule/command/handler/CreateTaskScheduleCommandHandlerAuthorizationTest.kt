/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateTaskScheduleCommandHandlerAuthorizationTest : AbstractTaskScheduleAuthorizationTest() {

  @Autowired private lateinit var createTaskScheduleCommandHandler: CreateTaskScheduleCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `create permission is granted to`(userType: UserTypeAccess) {
    val taskSchedule = SaveTaskScheduleDto(LocalDate.now(), null, null)

    checkAccessWith(userType) {
      Assertions.assertThat(
              createTaskScheduleCommandHandler.handle(
                  CreateTaskScheduleCommand(
                      identifier = TaskScheduleId(),
                      taskIdentifier = taskWithoutScheduleIdentifier,
                      start = taskSchedule.start,
                      end = taskSchedule.end,
                      slots = taskSchedule.slots)))
          .isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `create permission is denied for non-existing task`(userType: UserTypeAccess) {
    val taskSchedule = SaveTaskScheduleDto(LocalDate.now(), null, null)

    checkAccessWith(userType) {
      createTaskScheduleCommandHandler.handle(
          CreateTaskScheduleCommand(
              identifier = TaskScheduleId(),
              taskIdentifier = TaskId(),
              start = taskSchedule.start,
              end = taskSchedule.end,
              slots = taskSchedule.slots))
    }
  }
}
