/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TaskRescheduleServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskRescheduleService

  private val authorizeCriteria by lazy { SearchTasksDto(projectIdentifier = project.identifier) }
  private val nonAuthorizeCriteria by lazy { SearchTasksDto(projectIdentifier = ProjectId()) }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify validation authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.validate(authorizeCriteria) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify validation not authorized for non existing project`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.validate(nonAuthorizeCriteria) }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify reschedule authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.reschedule(2L, authorizeCriteria) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify reschedule not authorized for non existing project`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.reschedule(2L, nonAuthorizeCriteria) }
}
