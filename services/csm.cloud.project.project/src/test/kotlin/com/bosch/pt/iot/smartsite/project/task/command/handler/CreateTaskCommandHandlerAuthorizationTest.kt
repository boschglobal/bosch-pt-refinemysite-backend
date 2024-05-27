/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateTaskCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreateTaskCommandHandler

  private val projectCraftId by lazy { getIdentifier("projectCraft").asProjectCraftId() }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify creating a task is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { createAndAssignTask(project.identifier, null) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify creating a task is denied for non-existing project for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { createAndAssignTask(ProjectId(), null) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify creating a task and assign is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { createAndAssignTask(project.identifier, "participantFmAssignee") }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify creating a task and assign is denied for non-existing project for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { createAndAssignTask(ProjectId(), "participantFmAssignee") }
  }

  private fun createAndAssignTask(projectIdentifier: ProjectId, assigneeReference: String?) {
    val createTaskCommand =
        CreateTaskCommand(
            TaskId(),
            projectIdentifier,
            "task1",
            "description",
            "location",
            projectCraftId,
            assigneeReference?.let { getIdentifier(it).asParticipantId() },
            null,
            DRAFT)
    cut.handle(createTaskCommand)
  }
}
