/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import java.util.UUID
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface TaskRepository :
    MongoRepository<Task, AggregateIdentifier>,
    ShardedSaveOperation<Task, AggregateIdentifier>,
    ProjectContextOperationsExtension<Task>,
    TaskRepositoryExtension {

  @Query("{'_class': Task}") override fun findAll(): List<Task>

  @DeleteQuery("{'_class': Task}") override fun deleteAll()

  fun findFirstByIdentifierIdentifierOrderByIdentifierVersionDesc(identifier: UUID): Task?
}

fun TaskRepository.findLatest(identifier: UUID) =
    findFirstByIdentifierIdentifierOrderByIdentifierVersionDesc(identifier)
