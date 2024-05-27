/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface RfvCustomizationRepository :
    MongoRepository<RfvCustomization, AggregateIdentifier>,
    ShardedSaveOperation<RfvCustomization, AggregateIdentifier>,
    ProjectContextOperationsExtension<RfvCustomization>,
    RfvCustomizationRepositoryExtension {

  @Query("{'_class': RfvCustomization}") override fun findAll(): List<RfvCustomization>

  @DeleteQuery("{'_class': RfvCustomization}") override fun deleteAll()
}
