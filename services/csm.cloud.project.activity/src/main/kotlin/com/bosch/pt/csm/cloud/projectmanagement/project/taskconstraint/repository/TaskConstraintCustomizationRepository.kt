/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintCustomization
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface TaskConstraintCustomizationRepository :
    MongoRepository<TaskConstraintCustomization, AggregateIdentifier>,
    ShardedSaveOperation<TaskConstraintCustomization, AggregateIdentifier>,
    ProjectContextOperationsExtension<TaskConstraintCustomization>,
    TaskConstraintCustomizationRepositoryExtension {

  @Query("{'_class': TaskConstraintCustomization}")
  override fun findAll(): List<TaskConstraintCustomization>

  @DeleteQuery("{'_class': TaskConstraintCustomization}") override fun deleteAll()
}
