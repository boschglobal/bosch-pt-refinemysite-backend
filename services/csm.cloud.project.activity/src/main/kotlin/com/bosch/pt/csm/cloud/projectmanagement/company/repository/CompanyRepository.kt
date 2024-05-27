/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository

interface CompanyRepository :
    MongoRepository<Company, AggregateIdentifier>,
    ShardedSaveOperation<Company, AggregateIdentifier> {

  fun deleteByCompanyIdentifier(companyIdentifier: UUID)

  fun deleteByIdentifierIdentifierAndIdentifierVersion(identifier: UUID, version: Long)
}
