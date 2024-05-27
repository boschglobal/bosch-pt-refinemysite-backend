/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintService
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.UpdateTaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM1
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskConstraintAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskConstraintService

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find all task constraints is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAll(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find all task constraints for unknown project is denied for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAll(ProjectId()) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify resolve project constraints is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.resolveProjectConstraints(project.identifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify resolve project constraints for unknown project is denied for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.resolveProjectConstraints(ProjectId()) }
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify update task constraints is authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.update(UpdateTaskConstraintDto(project.identifier, CUSTOM1, true, "Custom constraint"))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify update task constraints for unknown project is denied for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.update(UpdateTaskConstraintDto(ProjectId(), CUSTOM1, true, "Custom constraint"))
    }
  }
}
