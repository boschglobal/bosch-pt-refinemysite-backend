/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.AssignTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateTaskCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var assignTaskCommandHandler: AssignTaskCommandHandler

  @Autowired private lateinit var cut: UpdateTaskCommandHandler

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify editing a task that is non-existing is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      updateTask(taskUnassigned, TaskId(), ETag.from(taskUnassigned.version))
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify editing a task that is created by own company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {

    // reassign task to creator to so that creator == assignee
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantCreator") }
    checkAccessWith(userType) { updateTask(taskAssigned, taskAssigned.identifier, ETag.from("1")) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify editing a task that is created by own company and assigned to other company is authorized for`(
      userType: UserTypeAccess
  ) {

    // reassign task to other company's FM
    doWithAuthorization(userMap[CSM]!!) { assignTask(taskAssigned, "participantOtherCompanyFm") }
    checkAccessWith(userType) { updateTask(taskAssigned, taskAssigned.identifier, ETag.from("1")) }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrWithAccess")
  @DisplayName(
      "verify editing a task that is created by own company and assigned to other participant of own " +
          "company is authorized for")
  fun `verify update and assign task authorized when created by own but assigned to other participant`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      updateTask(taskAssigned, taskAssigned.identifier, ETag.from(taskAssigned.version))
    }
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify editing a task that is created by own company and un-assigned is authorized for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      updateTask(taskUnassigned, taskUnassigned.identifier, ETag.from(taskUnassigned.version))
    }
  }

  private fun updateTask(task: Task, taskIdentifier: TaskId, eTag: ETag) {
    val taskDto =
        UpdateTaskCommand(
            taskIdentifier,
            task.project.identifier,
            eTag.toVersion(),
            "Updated name",
            task.description,
            task.location,
            task.projectCraft.identifier,
            if (task.workArea == null) null else task.workArea!!.identifier,
            if (task.assignee == null) null else task.assignee!!.identifier,
            task.status)
    cut.handle(taskDto)
  }

  private fun assignTask(task: Task, assigneeReference: String) =
      assignTaskCommandHandler.handle(
          task.identifier, getIdentifier(assigneeReference).asParticipantId())
}
