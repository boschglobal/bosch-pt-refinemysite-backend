/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch

import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskStatusCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateImportedTaskStatusBatchCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler
  @Autowired
  private lateinit var updateImportedTaskStatusBatchCommandHandler:
      UpdateImportedTaskStatusBatchCommandHandler

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify start draft task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, taskAssigned.assignee!!) }
    checkAccessWith(userType) { updateImportedTaskStatus(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify close draft task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, taskAssigned.assignee!!) }
    checkAccessWith(userType) { updateImportedTaskStatus(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify accept a draft task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, taskAssigned.assignee!!) }
    checkAccessWith(userType) { updateImportedTaskStatus(taskAssigned) }
  }

  private fun assignTask(task: Task, assignee: Participant) =
      assignTaskCommandHandler.handle(task.identifier, assignee.identifier)

  private fun updateImportedTaskStatus(task: Task) =
      updateImportedTaskStatusBatchCommandHandler.handle(
          task.project.identifier, listOf(UpdateTaskStatusCommand(task.identifier, task.status)))
}
