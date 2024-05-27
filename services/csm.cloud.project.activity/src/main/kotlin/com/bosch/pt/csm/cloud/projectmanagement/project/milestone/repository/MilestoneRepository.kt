/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model.Milestone
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface MilestoneRepository :
    MongoRepository<Milestone, AggregateIdentifier>,
    ShardedSaveOperation<Milestone, AggregateIdentifier>,
    ProjectContextOperationsExtension<Milestone>,
    MilestoneRepositoryExtension {

  @Query("{'_class': Milestone}") override fun findAll(): List<Milestone>

  @DeleteQuery("{'_class': Milestone}") override fun deleteAll()
}
