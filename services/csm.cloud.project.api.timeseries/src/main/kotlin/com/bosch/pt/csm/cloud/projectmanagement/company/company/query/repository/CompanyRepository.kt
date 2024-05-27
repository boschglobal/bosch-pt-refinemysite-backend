/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import org.springframework.data.mongodb.repository.MongoRepository

interface CompanyRepository : MongoRepository<Company, CompanyId> {

  fun findOneByIdentifier(id: CompanyId): Company?

  fun findAllByIdentifierIn(
      companies: List<CompanyId>,
  ): List<Company>

  fun findAllByIdentifierInAndDeletedFalse(
      companies: List<CompanyId>,
  ): List<Company>
}
