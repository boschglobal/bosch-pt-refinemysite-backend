/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Company
import org.springframework.data.mongodb.repository.MongoRepository

interface CompanyRepository :
    MongoRepository<Company, AggregateIdentifier>,
    ShardedSaveOperation<Company, AggregateIdentifier>,
    CompanyRepositoryExtension
