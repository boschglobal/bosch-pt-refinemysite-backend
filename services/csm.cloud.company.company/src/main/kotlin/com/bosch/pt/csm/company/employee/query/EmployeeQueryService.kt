/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.query

import com.bosch.pt.csm.application.security.AdminAuthorization
import com.bosch.pt.csm.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.csm.company.authorization.CompanyAuthorizer
import com.bosch.pt.csm.company.authorization.UserAuthorizer
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.company.employee.shared.repository.EmployeeRepository
import com.bosch.pt.csm.user.authorization.boundary.AdminUserAuthorizationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.unsorted
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmployeeQueryService(
    private val employeeRepository: EmployeeRepository,
    private val companyAuthorizer: CompanyAuthorizer,
    private val userAuthorizer: UserAuthorizer,
    private val adminUserAuthorizationService: AdminUserAuthorizationService
) {

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findAllByCompany(company: Company, pageable: Pageable): Page<Employee> {
    val translatedPageable: Pageable =
        filterAndTranslate(pageable, EMPLOYEE_BY_COMPANY_ALLOWED_SORTING_PROPERTIES)

    return adminUserAuthorizationService
        .getRestrictedCountries()
        .let {
          employeeRepository.findAllEmployeeIdentifiersByCompany(company, it, translatedPageable)
        }
        .let { findAllWithDetailsByIdentifiers(it, translatedPageable) }
  }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findEmployeeByUserId(identifier: UserId): Employee? =
      employeeRepository.findOneByUserRef(identifier)

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun countAllByCompanyIdentifier(identifier: CompanyId): Int =
      employeeRepository.countAllByCompanyIdentifier(identifier)

  @AdminAuthorization
  @Transactional(readOnly = true)
  fun findEmployeeWithDetailsByIdentifier(identifier: EmployeeId): Employee =
      employeeRepository.findOneWithDetailsByIdentifier(identifier)?.apply {
        companyAuthorizer.assertAuthorizedToAccessCompany(this.company)
        userAuthorizer.assertAuthorizedToAccessUser(this.userRef)
      }
          ?: throw AggregateNotFoundException(
              EMPLOYEE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  private fun findAllWithDetailsByIdentifiers(
      identifiers: Page<EmployeeId>,
      pageable: Pageable
  ): Page<Employee> =
      employeeRepository
          .findAllWithDetailsByIdentifierIn(identifiers.toSet(), pageable.getSortOr(unsorted()))
          .let { PageImpl(it, pageable, identifiers.totalElements) }

  companion object {

    val EMPLOYEE_BY_COMPANY_ALLOWED_SORTING_PROPERTIES: MutableMap<String, String> =
        mutableMapOf(
            Pair("user.firstName", "userRef.firstName"),
            Pair("user.lastName", "userRef.lastName"),
            Pair("user.email", "userRef.email"))
  }
}
