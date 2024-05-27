/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class StartTaskCommandHandlerAuthorizationTest : AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler

  @Autowired private lateinit var sendTaskCommandHandler: SendTaskCommandHandler
  @Autowired private lateinit var startTaskCommandHandler: StartTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndAssignedFmWithAccess")
  fun `verify start a task that CR is assigned to company and FM is assigned to task`(
      userType: UserTypeAccess
  ) {
    taskAssigned.status = OPEN
    doWithAuthorization(userMap[CSM]!!) {
      assignTask(taskAssigned, taskAssigned.assignee!!)
      sendTask(taskAssigned)
    }
    checkAccessWith(userType) { startTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyWithAccess")
  fun `verify start a task that CR is not assigned to company and FM is not assigned to task`(
      userType: UserTypeAccess
  ) {
    taskAssigned.status = OPEN
    doWithAuthorization(userMap[CSM]!!) {
      assignTask(taskAssigned, "participantOtherCompanyFm")
      sendTask(taskAssigned)
    }
    checkAccessWith(userType) { startTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyCrWithAccess")
  fun `verify start a task that is assigned to an FM that was reassigned to another company`(
      userType: UserTypeAccess
  ) {
    eventStreamGenerator.submitTask("newTask") {
      it.assignee = getByReference("participantFmReassignedBefore")
      it.status = TaskStatusEnumAvro.OPEN
    }

    checkAccessWith(userType) {
      startTaskCommandHandler.handle(getIdentifier("newTask").asTaskId())
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify start draft task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, taskAssigned.assignee!!) }
    checkAccessWith(userType) { startTask(taskAssigned) }
  }

  private fun assignTask(task: Task, assigneeReference: String) =
      assignTaskCommandHandler.handle(
          task.identifier, getIdentifier(assigneeReference).asParticipantId())

  private fun assignTask(task: Task, assignee: Participant) =
      assignTaskCommandHandler.handle(task.identifier, assignee.identifier)

  private fun sendTask(task: Task) = sendTaskCommandHandler.handle(task.identifier)

  private fun startTask(task: Task) = startTaskCommandHandler.handle(task.identifier)
}
