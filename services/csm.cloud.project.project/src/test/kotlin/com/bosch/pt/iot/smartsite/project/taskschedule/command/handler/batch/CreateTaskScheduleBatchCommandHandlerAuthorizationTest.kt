/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleBatchDto
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.AbstractTaskScheduleAuthorizationTest
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateTaskScheduleBatchCommandHandlerAuthorizationTest :
    AbstractTaskScheduleAuthorizationTest() {

  @Autowired
  private lateinit var createTaskScheduleBatchCommandHandler: CreateTaskScheduleBatchCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `create multiple permission is granted to`(userType: UserTypeAccess) {
    val taskSchedules: MutableCollection<SaveTaskScheduleBatchDto> = ArrayList()
    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            null, null, taskWithoutScheduleIdentifier, LocalDate.now(), null, null))
    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            null, null, taskWithoutScheduleIdentifier2, null, LocalDate.now().plusDays(3), null))

    checkAccessWith(userType) {
      createTaskScheduleBatchCommandHandler.handle(
          taskSchedules.map {
            CreateTaskScheduleCommand(
                identifier = it.identifier ?: TaskScheduleId(),
                taskIdentifier = it.taskIdentifier,
                start = it.start,
                end = it.end,
                slots = it.slots)
          })
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `create multiple permission is denied for non-existing task`(userType: UserTypeAccess) {
    val taskSchedules: MutableCollection<SaveTaskScheduleBatchDto> = ArrayList()
    taskSchedules.add(
        SaveTaskScheduleBatchDto(
            null, null, taskWithoutScheduleIdentifier, LocalDate.now(), null, null))
    taskSchedules.add(
        SaveTaskScheduleBatchDto(null, null, TaskId(), null, LocalDate.now().plusDays(3), null))

    checkAccessWith(userType) {
      createTaskScheduleBatchCommandHandler.handle(
          taskSchedules.map {
            CreateTaskScheduleCommand(
                identifier = it.identifier ?: TaskScheduleId(),
                taskIdentifier = it.taskIdentifier,
                start = it.start,
                end = it.end,
                slots = it.slots)
          })
    }
  }
}
