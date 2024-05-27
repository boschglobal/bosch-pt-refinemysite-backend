/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.company.command.api.UpdateCompanyCommand
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshot
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshotStore
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateCompanyCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val companyAuthorizer: CompanyAuthorizer,
    private val snapshotStore: CompanySnapshotStore
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: UpdateCompanyCommand) {
    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .checkAuthorization { isUserAuthorizedToAccessCompany(it) }
        .onFailureThrow { AccessDeniedException("Unauthorized to access company of that country") }
        .checkPrecondition { isUserAuthorizedForCountries(command) }
        .onFailureThrow(COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY)
        .update {
          it.copy(
              name = command.name,
              streetAddress = command.streetAddress,
              postBoxAddress = command.postBoxAddress)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
  }

  private fun isUserAuthorizedToAccessCompany(company: CompanySnapshot) =
      companyAuthorizer.isUserAuthorizedForCountries(
          setOf(company.postBoxAddress?.country, company.streetAddress?.country))

  private fun isUserAuthorizedForCountries(command: UpdateCompanyCommand) =
      companyAuthorizer.isUserAuthorizedForCountries(
          setOf(command.postBoxAddress?.country, command.streetAddress?.country))
}
