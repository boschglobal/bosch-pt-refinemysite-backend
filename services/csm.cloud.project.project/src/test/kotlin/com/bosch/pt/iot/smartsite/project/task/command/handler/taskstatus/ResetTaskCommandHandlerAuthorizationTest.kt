/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus

import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ResetTaskCommandHandlerAuthorizationTest : AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler

  @Autowired private lateinit var sendTaskCommandHandler: SendTaskCommandHandler
  @Autowired private lateinit var startTaskCommandHandler: StartTaskCommandHandler
  @Autowired private lateinit var closeTaskCommandHandler: CloseTaskCommandHandler
  @Autowired private lateinit var resetTaskCommandHandler: ResetTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndAssignedFmWithAccess")
  fun `verify reset started task authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) {
      assignTask(taskAssigned, taskAssigned.assignee!!)
      sendTask(taskAssigned)
      startTask(taskAssigned)
    }
    checkAccessWith(userType) { resetTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndAssignedFmWithAccess")
  fun `verify reset closed task authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) {
      assignTask(taskAssigned, taskAssigned.assignee!!)
      sendTask(taskAssigned)
      startTask(taskAssigned)
      closeTask(taskAssigned)
    }
    checkAccessWith(userType) { resetTask(taskAssigned) }
  }

  private fun assignTask(task: Task, assignee: Participant) =
      assignTaskCommandHandler.handle(task.identifier, assignee.identifier)

  private fun sendTask(task: Task) = sendTaskCommandHandler.handle(task.identifier)

  private fun startTask(task: Task) = startTaskCommandHandler.handle(task.identifier)

  private fun closeTask(task: Task) = closeTaskCommandHandler.handle(task.identifier)

  private fun resetTask(task: Task) = resetTaskCommandHandler.handle(task.identifier)
}
