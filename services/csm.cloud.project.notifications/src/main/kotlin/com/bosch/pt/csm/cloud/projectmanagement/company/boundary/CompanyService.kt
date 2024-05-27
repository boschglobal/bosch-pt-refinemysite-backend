/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.boundary

import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.CompanyRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class CompanyService(private val companyRepository: CompanyRepository) {

  @Trace fun save(company: Company): Company = companyRepository.save(company)

  @Trace
  fun deleteCompany(companyIdentifier: UUID) = companyRepository.deleteCompany(companyIdentifier)
}
