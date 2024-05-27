/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_EXIST_COMPANY_EMPLOYEE
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.company.command.api.DeleteCompanyCommand
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshot
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshotStore
import com.bosch.pt.csm.company.employee.query.EmployeeQueryService
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteCompanyCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val companyAuthorizer: CompanyAuthorizer,
    private val snapshotStore: CompanySnapshotStore,
    private val employeeQueryService: EmployeeQueryService
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: DeleteCompanyCommand) {
    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .checkAuthorization { isUserAuthorizedToAccessCompany(it) }
        .onFailureThrow { AccessDeniedException("Unauthorized to access company of that country") }
        .checkPrecondition { doesNotContainsEmployees(it) }
        .onFailureThrow(COMPANY_VALIDATION_ERROR_EXIST_COMPANY_EMPLOYEE)
        .emitEvent(DELETED)
        .to(eventBus)
  }

  private fun isUserAuthorizedToAccessCompany(company: CompanySnapshot) =
      companyAuthorizer.isUserAuthorizedForCountries(
          setOf(company.postBoxAddress?.country, company.streetAddress?.country))

  private fun doesNotContainsEmployees(company: CompanySnapshot) =
      employeeQueryService.countAllByCompanyIdentifier(company.identifier) == 0
}
