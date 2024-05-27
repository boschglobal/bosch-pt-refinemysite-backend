/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.common.repository.SortCriteriaFilter.filterAndTranslate
import com.bosch.pt.csm.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.user.user.boundary.dto.UserFilterCriteriaDto
import com.bosch.pt.csm.user.user.model.dto.UserWithEmployeeCompanySearchResultDto
import datadog.trace.api.Trace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmployableUserQueryService(
    private val repository: EmployableUserProjectionRepository,
    private val adminUserAuthorizationService: AdminUserAuthorizationService,
) {

  @Trace
  @Transactional(readOnly = true)
  fun search(
      filter: UserFilterCriteriaDto,
      pageable: Pageable
  ): Page<UserWithEmployeeCompanySearchResultDto> =
      adminUserAuthorizationService.getRestrictedCountries().let {
        repository.findAllBySearchCriteria(
            filter.name,
            filter.email,
            filter.companyName,
            it,
            filterAndTranslate(pageable, ALLOWED_SORTING_PROPERTIES))
      }

  companion object {
    val ALLOWED_SORTING_PROPERTIES =
        mapOf(
            Pair("user.createdAt", "userCreatedDate"),
            Pair("user.email", "email"),
            Pair("user.firstName", "userName"),
            Pair("company.displayName", "companyName"))
  }
}
