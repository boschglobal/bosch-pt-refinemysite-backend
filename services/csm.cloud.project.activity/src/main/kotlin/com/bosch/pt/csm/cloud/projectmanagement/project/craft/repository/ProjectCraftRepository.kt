/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.model.ProjectCraft
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ProjectCraftRepository :
    MongoRepository<ProjectCraft, AggregateIdentifier>,
    ShardedSaveOperation<ProjectCraft, AggregateIdentifier>,
    ProjectContextOperationsExtension<ProjectCraft> {

  @Query("{'_class': ProjectCraft}") override fun findAll(): List<ProjectCraft>

  @DeleteQuery("{'_class': ProjectCraft}") override fun deleteAll()
}
