/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.eventlistener

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_EMPLOYEE_NOT_FOUND
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.boundary.EmployeeQueryService
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignFirstParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.AssignFirstParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CreateFirstCsmEventListener(
    private val employeeQueryService: EmployeeQueryService,
    private val assignFirstParticipantCommandHandler: AssignFirstParticipantCommandHandler
) {

  @EventListener
  fun handle(event: ProjectEventAvro) {
    if (event.name == CREATED) {
      val employee: Employee =
          employeeQueryService.findOneByUser(getCurrentUser())
              ?: throw PreconditionViolationException(PROJECT_VALIDATION_ERROR_EMPLOYEE_NOT_FOUND)

      assignFirstParticipantCommandHandler.handle(
          AssignFirstParticipantCommand(
              projectRef = event.aggregate.getIdentifier().asProjectId(),
              companyRef = employee.company!!.identifier!!.asCompanyId(),
              userRef = employee.user!!.identifier!!.asUserId()))
    }
  }
}
