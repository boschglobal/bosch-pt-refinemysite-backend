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
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class AcceptTaskCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler

  @Autowired private lateinit var acceptTaskCommandHandler: AcceptTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify accept a draft task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, taskAssigned.assignee!!) }
    checkAccessWith(userType) { acceptTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify accept an open task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) {
      eventStreamGenerator.submitTask("newTask") {
        it.assignee = getByReference("participantCreator")
        it.status = TaskStatusEnumAvro.OPEN
      }
    }
    checkAccessWith(userType) {
      acceptTask(repositories.findTaskWithDetails(getIdentifier("newTask").asTaskId())!!)
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify accept an started task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) {
      eventStreamGenerator.submitTask("newTask") {
        it.assignee = getByReference("participantCreator")
        it.status = TaskStatusEnumAvro.STARTED
      }
    }
    checkAccessWith(userType) {
      acceptTask(repositories.findTaskWithDetails(getIdentifier("newTask").asTaskId())!!)
    }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify accept an closed task is authorized for`(userType: UserTypeAccess) {
    doWithAuthorization(userMap[CSM]!!) {
      eventStreamGenerator.submitTask("newTask") {
        it.assignee = getByReference("participantCreator")
        it.status = TaskStatusEnumAvro.CLOSED
      }
    }
    checkAccessWith(userType) {
      acceptTask(repositories.findTaskWithDetails(getIdentifier("newTask").asTaskId())!!)
    }
  }

  private fun assignTask(task: Task, assignee: Participant) =
      assignTaskCommandHandler.handle(task.identifier, assignee.identifier)

  private fun acceptTask(task: Task) = acceptTaskCommandHandler.handle(task.identifier)
}
