/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.CREATED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_EXIST_USER_EMPLOYEE
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.authorization.UserAuthorizer
import com.bosch.pt.csm.company.employee.command.api.CreateEmployeeCommand
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.command.snapshotstore.EmployeeSnapshot
import com.bosch.pt.csm.company.employee.shared.repository.EmployeeRepository
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Component
class CreateEmployeeCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val idGenerator: IdGenerator,
    private val companyAuthorizer: CompanyAuthorizer,
    private val userAuthorizer: UserAuthorizer,
    private val employeeRepository: EmployeeRepository
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: CreateEmployeeCommand): EmployeeId {

    return EmployeeSnapshot(
            identifier = command.identifier ?: EmployeeId(idGenerator.generateId()),
            userRef = command.userRef,
            companyRef = command.companyRef,
            roles = command.roles)
        .toCommandHandler()
        .checkPrecondition { noEmployeeExistsYetForReferencedUser(command.userRef) }
        .onFailureThrow(EMPLOYEE_VALIDATION_ERROR_EXIST_USER_EMPLOYEE)
        .checkPrecondition { companyAuthorizer.isUserAuthorizedToAccessCompany(command.companyRef) }
        .onFailureThrow(COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY)
        .checkPrecondition { userAuthorizer.isUserAuthorizedForCountry(command.userRef) }
        .onFailureThrow(EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY)
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun noEmployeeExistsYetForReferencedUser(userRef: UserId) =
      employeeRepository.findOneByUserRef(userRef) == null
}
