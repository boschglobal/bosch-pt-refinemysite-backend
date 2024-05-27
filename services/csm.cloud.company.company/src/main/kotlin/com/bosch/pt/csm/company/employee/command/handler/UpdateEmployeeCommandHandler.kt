/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.UPDATED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.authorization.UserAuthorizer
import com.bosch.pt.csm.company.employee.command.api.UpdateEmployeeCommand
import com.bosch.pt.csm.company.employee.command.snapshotstore.EmployeeSnapshotStore
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateEmployeeCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val snapshotStore: EmployeeSnapshotStore,
    private val companyAuthorizer: CompanyAuthorizer,
    private val userAuthorizer: UserAuthorizer
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: UpdateEmployeeCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .checkAuthorization { companyAuthorizer.isUserAuthorizedToAccessCompany(it.companyRef) }
          .onFailureThrow(COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY)
          .checkAuthorization { userAuthorizer.isUserAuthorizedForCountry(it.userRef) }
          .onFailureThrow(EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY)
          .assertVersionMatches(command.version)
          .update { it.copy(roles = command.roles.toMutableList()) }
          .emitEvent(UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}
