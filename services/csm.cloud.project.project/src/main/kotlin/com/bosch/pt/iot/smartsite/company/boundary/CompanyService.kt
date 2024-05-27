/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMPANY_VALIDATION_ERROR_MISSING_ADDRESS
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class CompanyService(
    private val companyRepository: CompanyRepository,
    private val participantQueryService: ParticipantQueryService
) {

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByIdentifier(identifier: UUID): Company? =
      companyRepository.findOneWithDetailsByIdentifier(identifier)

  @NoPreAuthorize
  @Transactional
  open fun save(company: Company): UUID {

    // Check if either a street or post box address is specified for the company
    checkAndPrepareCompanyAddress(company)

    val existingCompany = companyRepository.findOneByIdentifier(requireNotNull(company.identifier))
    if (existingCompany != null) {
      company.id = existingCompany.id
    }

    return companyRepository.save(company).identifier!!
  }

  @NoPreAuthorize
  @Transactional
  open fun deleteCompany(companyIdentifier: UUID, version: Long) {
    val company = companyRepository.findOneByIdentifier(companyIdentifier) ?: return
    if (participantQueryService.countAllByCompany(company) > 0 && !company.deleted) {
      company.version = version
      company.deleted = true
      companyRepository.save(company)
    } else if (!company.deleted) {
      companyRepository.delete(company)
    }
  }

  private fun checkAndPrepareCompanyAddress(company: Company) {
    if (company.streetAddress == null && company.postBoxAddress == null) {
      throw PreconditionViolationException(COMPANY_VALIDATION_ERROR_MISSING_ADDRESS)
    }
  }
}
