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
import com.bosch.pt.csm.cloud.projectmanagement.company.model.Employee
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.mongodb.repository.MongoRepository

interface EmployeeRepository :
    MongoRepository<Employee, AggregateIdentifier>,
    ShardedSaveOperation<Employee, AggregateIdentifier>,
    EmployeeRepositoryExtension {

    @Cacheable(cacheNames = ["employee"])
    fun findOneCachedByIdentifier(identifier: AggregateIdentifier): Employee
}
