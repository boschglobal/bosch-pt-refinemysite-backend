/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro.DELETED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.authorization.UserAuthorizer
import com.bosch.pt.csm.company.employee.command.api.DeleteEmployeeCommand
import com.bosch.pt.csm.company.employee.command.snapshotstore.EmployeeSnapshotStore
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteEmployeeCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val snapshotStore: EmployeeSnapshotStore,
    private val userAuthorizer: UserAuthorizer,
    private val companyAuthorizer: CompanyAuthorizer,
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: DeleteEmployeeCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .checkAuthorization { companyAuthorizer.isUserAuthorizedToAccessCompany(it.companyRef) }
          .onFailureThrow(COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY)
          .checkPrecondition { userAuthorizer.isUserAuthorizedForCountry(it.userRef) }
          .onFailureThrow(EMPLOYEE_VALIDATION_ERROR_UNAUTHORIZED_FOR_USER_OF_THAT_COUNTRY)
          .emitEvent(DELETED)
          .to(eventBus)

  @DenyWebRequests
  @PreAuthorize("isAuthenticated()")
  @Transactional
  fun handleOnUserDelete(command: DeleteEmployeeCommand) =
      snapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .emitEvent(DELETED)
          .to(eventBus)
}
