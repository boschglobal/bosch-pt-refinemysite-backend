/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.authorization

import com.bosch.pt.csm.common.exceptions.ReferencedEntityNotFoundException
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.company.shared.repository.CompanyRepository
import com.bosch.pt.csm.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.user.authorization.boundary.toSetOfAlternativeCountryNames
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component
class CompanyAuthorizer(
    private val adminUserAuthorizationService: AdminUserAuthorizationService,
    private val companyRepository: CompanyRepository
) {

  fun assertAuthorizedToAccessCompany(company: Company) {
    if (!isUserAuthorizedForCountries(
        setOf(company.postBoxAddress?.country, company.streetAddress?.country))) {
      throw AccessDeniedException("Unauthorized to access company of that country")
    }
  }

  fun isUserAuthorizedForCountries(countryNames: Set<String?>): Boolean =
      adminUserAuthorizationService.getRestrictedCountries().toSetOfAlternativeCountryNames().let {
        it.isEmpty() || it.intersect(countryNames).isNotEmpty()
      }

  fun isUserAuthorizedToAccessCompany(companyId: CompanyId): Boolean =
      companyRepository.findOneByIdentifier(companyId).let {
        if (it != null) isUserAuthorizedToAccessCompany(it)
        else throw ReferencedEntityNotFoundException(COMPANY_VALIDATION_ERROR_NOT_FOUND)
      }

  fun isUserAuthorizedToAccessCompany(company: Company) =
      isUserAuthorizedForCountries(
          setOf(company.postBoxAddress?.country, company.streetAddress?.country))
}
