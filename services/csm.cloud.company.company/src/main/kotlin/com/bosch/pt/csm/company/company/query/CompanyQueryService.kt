/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.query

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.company.shared.repository.CompanyRepository
import com.bosch.pt.csm.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.user.authorization.boundary.toSetOfAlternativeCountryNames
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyQueryService(
    private val companyRepository: CompanyRepository,
    private val companyAuthorizer: CompanyAuthorizer,
    private val adminUserAuthorizationService: AdminUserAuthorizationService
) {

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findCompaniesForFilters(name: String?, pageable: Pageable): Page<Company> =
      adminUserAuthorizationService.getRestrictedCountries().toSetOfAlternativeCountryNames().let {
        if (name == null) {
          companyRepository.findAll(
              it, filterAndTranslate(pageable, SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES))
        } else {
          companyRepository.findAllByNameContainingIgnoreCase(
              name, it, filterAndTranslate(pageable, SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES))
        }
      }

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findAllCompanies(pageable: Pageable): Page<Company> =
      adminUserAuthorizationService.getRestrictedCountries().toSetOfAlternativeCountryNames().let {
        companyRepository.findAll(
            it, filterAndTranslate(pageable, SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES))
      }

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findAllCompaniesWithEmployee(companyIdentifiers: List<CompanyId>): List<CompanyId> =
      // authorization service not required here since this is only used by the factory to filter
      // identifiers
      companyRepository.findAllCompaniesWithEmployee(companyIdentifiers)

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun suggestCompaniesByTerm(term: String?, pageable: Pageable): Page<Company> =
      adminUserAuthorizationService.getRestrictedCountries().toSetOfAlternativeCountryNames().let {
        companyRepository.suggestCompaniesByTerm(
            term, it, filterAndTranslate(pageable, SUGGEST_COMPANIES_ALLOWED_SORTING_PROPERTIES))
      }

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findCompanyByIdentifier(identifier: CompanyId): Company =
      companyRepository.findOneByIdentifier(identifier)?.also {
        companyAuthorizer.assertAuthorizedToAccessCompany(it)
      }
          ?: throw AggregateNotFoundException(
              COMPANY_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findCompanyWithDetailsByIdentifier(identifier: CompanyId): Company? =
      companyRepository.findOneWithDetailsByIdentifier(identifier)?.also {
        companyAuthorizer.assertAuthorizedToAccessCompany(it)
      }

  companion object {
    val SEARCH_COMPANIES_ALLOWED_SORTING_PROPERTIES: Map<String, String> = mapOf("name" to "name")
    val SUGGEST_COMPANIES_ALLOWED_SORTING_PROPERTIES: Map<String, String> =
        mapOf("displayName" to "name")
  }
}
