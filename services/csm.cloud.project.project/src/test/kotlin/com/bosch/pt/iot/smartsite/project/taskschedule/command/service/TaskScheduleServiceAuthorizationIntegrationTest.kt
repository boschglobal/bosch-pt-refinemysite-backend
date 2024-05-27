/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.service

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.AbstractTaskScheduleAuthorizationTest
import com.google.common.collect.Lists
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskScheduleServiceAuthorizationIntegrationTest : AbstractTaskScheduleAuthorizationTest() {

  @Autowired private lateinit var taskScheduleService: TaskScheduleService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `view permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(taskScheduleService.findByTaskIdentifier(taskWithScheduleIdentifier)).isNotNull
      assertThat(taskScheduleService.findWithDetailsByTaskIdentifier(taskWithScheduleIdentifier))
          .isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `view permission is denied for non-existing task`(userType: UserTypeAccess) {
    checkAccessWith(userType) { taskScheduleService.findByTaskIdentifier(TaskId()) }
    checkAccessWith(userType) { taskScheduleService.findWithDetailsByTaskIdentifier(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `search permission is granted to`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(
              taskScheduleService.findWithDetailsByTaskIdentifiers(
                  setOf(taskWithScheduleIdentifier)))
          .isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `search permission is denied for tasks of unauthorized projects`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(
              taskScheduleService.findWithDetailsByTaskIdentifiers(
                  Lists.newArrayList(taskWithScheduleIdentifier, otherProjectTaskIdentifier)))
          .isNotNull()
    }
  }
}
