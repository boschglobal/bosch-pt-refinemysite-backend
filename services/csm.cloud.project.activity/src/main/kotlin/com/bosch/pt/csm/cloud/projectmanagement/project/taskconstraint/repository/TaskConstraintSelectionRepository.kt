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
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintSelection
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface TaskConstraintSelectionRepository :
    MongoRepository<TaskConstraintSelection, AggregateIdentifier>,
    ShardedSaveOperation<TaskConstraintSelection, AggregateIdentifier>,
    ProjectContextOperationsExtension<TaskConstraintSelection> {

  @Query("{'_class': TaskAction}") override fun findAll(): List<TaskConstraintSelection>

  @DeleteQuery("{'_class': TaskAction}") override fun deleteAll()
}
