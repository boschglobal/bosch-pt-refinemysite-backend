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
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class SendTaskCommandHandlerAuthorizationTest : AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler

  @Autowired private lateinit var sendTaskCommandHandler: SendTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify sending a task that is created by own company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {
    doWithAuthorization(userCreator) {
      eventStreamGenerator.submitTask("newTask") {
        it.assignee = getByReference("participantCreator")
        it.status = TaskStatusEnumAvro.DRAFT
      }

      checkAccessWith(userType) {
        sendTask(repositories.findTaskWithDetails(getIdentifier("newTask").asTaskId())!!)
      }
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyWithAccess")
  fun `verify sending a task that is created by own company and assigned to other company is authorized for`(
      userType: UserTypeAccess
  ) {
    // reassign task to other company's FM
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantOtherCompanyFm") }
    checkAccessWith(userType) { sendTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyWithAccess")
  fun `verify sending a task that is created by other company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {
    // reassign task to other company's FM
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantOtherCompanyFm") }
    checkAccessWith(userType) { sendTask(taskAssigned) }
  }

  private fun assignTask(task: Task, assigneeReference: String) =
      assignTaskCommandHandler.handle(
          task.identifier, getIdentifier(assigneeReference).asParticipantId())

  private fun sendTask(task: Task) = sendTaskCommandHandler.handle(task.identifier)
}
