/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.service

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.repository.CompanyRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class CompanyQueryService(private val repository: CompanyRepository) {

  @Cacheable(cacheNames = ["companies-by-identifiers-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiersAndDeletedFalse(companyIds: List<CompanyId>): List<Company> =
      repository.findAllByIdentifierInAndDeletedFalse(companyIds)

  @Cacheable(cacheNames = ["companies-by-identifiers"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiers(companyIds: List<CompanyId>): List<Company> =
      repository.findAllByIdentifierIn(companyIds)
}
