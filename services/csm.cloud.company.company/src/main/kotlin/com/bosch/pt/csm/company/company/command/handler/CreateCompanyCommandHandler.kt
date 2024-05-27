/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.CREATED
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.command.api.CreateCompanyCommand
import com.bosch.pt.csm.company.company.command.snapshotstore.CompanySnapshot
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Component
class CreateCompanyCommandHandler(
    private val eventBus: CompanyContextLocalEventBus,
    private val idGenerator: IdGenerator,
    private val companyAuthorizer: CompanyAuthorizer
) {

  @AdminAuthorization
  @Transactional
  fun handle(command: CreateCompanyCommand): CompanyId =
      CompanySnapshot(
              identifier = command.identifier ?: CompanyId(idGenerator.generateId()),
              name = command.name,
              streetAddress = command.streetAddress,
              postBoxAddress = command.postBoxAddress)
          .toCommandHandler()
          .checkPrecondition { isUserAuthorizedForCountries(command) }
          .onFailureThrow(COMPANY_VALIDATION_ERROR_UNAUTHORIZED_TO_USE_COUNTRY)
          .emitEvent(CREATED)
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun isUserAuthorizedForCountries(command: CreateCompanyCommand) =
      companyAuthorizer.isUserAuthorizedForCountries(
          setOf(command.postBoxAddress?.country, command.streetAddress?.country))
}
