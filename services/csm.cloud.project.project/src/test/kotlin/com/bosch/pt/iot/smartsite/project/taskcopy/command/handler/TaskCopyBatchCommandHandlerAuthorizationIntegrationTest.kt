/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.TaskCopyCommand
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskCopyBatchCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskCopyBatchCommandHandler

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().setUserContext("userCsm")
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify copy tasks is authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(listOf(buildTaskCopyCommand()), project.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify copy tasks for non-existing project is denied for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(listOf(buildTaskCopyCommand()), ProjectId()) }

  private fun buildTaskCopyCommand() =
      TaskCopyCommand(
          copyFromIdentifier = getIdentifier("task").asTaskId(),
          shiftDays = 7L,
          includeDayCards = true)
}
