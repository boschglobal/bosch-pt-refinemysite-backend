/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.project.model.Project
import java.util.UUID
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ProjectRepository :
    MongoRepository<Project, AggregateIdentifier>,
    ShardedSaveOperation<Project, AggregateIdentifier> {

  @Query("{'_class': Project}") override fun findAll(): List<Project>

  @DeleteQuery("{'_class': Project}") override fun deleteAll()

  fun deleteByProjectIdentifier(projectIdentifier: UUID)

  fun deleteByIdentifierIdentifierAndIdentifierVersion(identifier: UUID, version: Long)
}
