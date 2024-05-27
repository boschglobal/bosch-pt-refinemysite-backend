/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.DeleteTaskScheduleCommand
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class DeleteTaskScheduleCommandHandlerAuthorizationTest : AbstractTaskScheduleAuthorizationTest() {

  @Autowired private lateinit var deleteTaskScheduleCommandHandler: DeleteTaskScheduleCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `delete permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      deleteTaskScheduleCommandHandler.handle(
          DeleteTaskScheduleCommand(taskIdentifier = taskWithScheduleIdentifier, ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `delete permission is denied for non-existing task`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      deleteTaskScheduleCommandHandler.handle(
          DeleteTaskScheduleCommand(taskIdentifier = TaskId(), ETag.from("0")))
    }
  }
}
