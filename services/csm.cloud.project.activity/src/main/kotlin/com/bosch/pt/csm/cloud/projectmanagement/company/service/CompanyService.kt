/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.service

import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import com.bosch.pt.csm.cloud.projectmanagement.company.repository.CompanyRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class CompanyService(private val companyRepository: CompanyRepository) {

  @Trace fun save(company: Company) = companyRepository.save(company)

  @Trace fun delete(identifier: UUID) = companyRepository.deleteByCompanyIdentifier(identifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long) =
      companyRepository.deleteByIdentifierIdentifierAndIdentifierVersion(identifier, version)
}
