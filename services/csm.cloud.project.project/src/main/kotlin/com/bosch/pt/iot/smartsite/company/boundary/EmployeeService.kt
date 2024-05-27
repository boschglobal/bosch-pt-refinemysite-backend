/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.boundary

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.participant.command.api.RemoveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.RemoveParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val participantQueryService: ParticipantQueryService,
    private val removeParticipantCommandHandler: RemoveParticipantCommandHandler,
    private val userService: UserService,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) {

  @NoPreAuthorize
  @Transactional
  open fun save(employee: Employee): UUID {
    val existingEmployee =
        employeeRepository.findOneByUserIdentifierAndCompanyIdentifier(
            requireNotNull(requireNotNull(employee.user).identifier),
            requireNotNull(requireNotNull(employee.company).identifier))
            ?: return employeeRepository.save(employee).identifier!!

    existingEmployee.roles = employee.roles
    existingEmployee.setCreatedBy(employee.createdBy.orElse(null))
    existingEmployee.setLastModifiedBy(employee.lastModifiedBy.orElse(null))
    existingEmployee.setCreatedDate(employee.createdDate.orElse(null))
    existingEmployee.setLastModifiedDate(employee.lastModifiedDate.orElse(null))
    existingEmployee.version = employee.version
    existingEmployee.identifier = employee.identifier

    return employeeRepository.save(existingEmployee).identifier!!
  }

  @DenyWebRequests
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteEmployee(employeeIdentifier: UUID) {
    val employee = employeeRepository.findOneByIdentifier(employeeIdentifier) ?: return
    deactivateAllParticipantsForEmployee(employee)
    employeeRepository.delete(employee)
  }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneForCurrentUser(): Employee? =
      employeeRepository.findOneByUserIdentifier(requireNotNull(getCurrentUser().identifier))

  private fun deactivateAllParticipantsForEmployee(employee: Employee) {
    val participants =
        participantQueryService.findAllParticipantsByCompanyAndUser(
            employee.company!!.identifier!!.asCompanyId(), employee.user!!.identifier!!.asUserId())

    if (participants.isNotEmpty()) {
      doWithAuthenticatedUser(userService.findOneByIdentifier(systemUserIdentifier)) {
        participants
            .filter { it.status != INACTIVE }
            .forEach {
              removeParticipantCommandHandler.handleFromMessaging(
                  RemoveParticipantCommand(it.identifier))
            }
      }
    }
  }
}
