/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment

import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.query.AbstractTaskServiceAuthorizationIntegrationTest
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UnassignTaskCommandHandlerAuthorizationTest :
    AbstractTaskServiceAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UnassignTaskCommandHandler

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify unassign a task that is created by own company and assigned to own company is authorized for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { unassignTask(taskAssigned) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify unassign a unknown task is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.handle(TaskId()) }
  }

  private fun unassignTask(task: Task) = cut.handle(task.identifier)
}
