/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.command.handler

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.reschedule.command.api.RescheduleCommand
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ValidateRescheduleCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: RescheduleCommandHandler

  private val authorizeCommand by lazy {
    RescheduleCommand(
        shiftDays = 2L,
        useTaskCriteria = true,
        useMilestoneCriteria = true,
        taskCriteria = SearchTasksDto(projectIdentifier = project.identifier),
        milestoneCriteria = SearchMilestonesDto(projectIdentifier = project.identifier),
        projectIdentifier = project.identifier)
  }

  private val nonAuthorizeCommand by lazy {
    val projectId = ProjectId()
    RescheduleCommand(
        shiftDays = 2L,
        useTaskCriteria = true,
        useMilestoneCriteria = true,
        taskCriteria = SearchTasksDto(projectIdentifier = projectId),
        milestoneCriteria = SearchMilestonesDto(projectIdentifier = projectId),
        projectIdentifier = projectId)
  }

  @ParameterizedTest
  @MethodSource("csmWithAccess")
  fun `verify validation authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(authorizeCommand) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify validation not authorized for non existing project`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(nonAuthorizeCommand) }
}
