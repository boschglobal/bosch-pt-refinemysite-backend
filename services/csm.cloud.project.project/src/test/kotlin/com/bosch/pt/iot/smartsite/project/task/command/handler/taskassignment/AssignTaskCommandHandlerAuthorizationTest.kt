/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class AssignTaskCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: AssignTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndAssignedFmWithAccess")
  fun `verify assigning a task that is created by own company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { assignTask(taskAssigned, "participantFmAssignee") }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyWithAccess")
  fun `verify assigning a task that is created by own company and assigned to other company is authorized for`(
      userType: UserTypeAccess
  ) {
    // reassign task to other company's FM
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantOtherCompanyFm") }
    checkAccessWith(userType) { assignTask(taskAssigned, "participantFmAssignee") }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify assigning a task that is created by own company and un-assigned is authorized for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { assignTask(taskUnassigned, "participantCreator") }
  }

  @ParameterizedTest
  @MethodSource("csmAndOtherCompanyWithAccess")
  fun `verify assigning a task that is created by other company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {
    // reassign task to other company's FM
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantOtherCompanyFm") }
    checkAccessWith(userType) { assignTask(taskAssigned, "participantOtherCompanyFm") }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify assigning a unknown task is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(TaskId(), getIdentifier("participantFmAssignee").asParticipantId())
    }
  }

  private fun assignTask(task: Task, assigneeReference: String) =
      cut.handle(task.identifier, getIdentifier(assigneeReference).asParticipantId())
}
