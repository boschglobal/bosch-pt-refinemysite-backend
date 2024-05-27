/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface TopicRepository :
    MongoRepository<Topic, AggregateIdentifier>,
    ShardedSaveOperation<Topic, AggregateIdentifier>,
    TopicRepositoryExtension,
    ProjectContextOperationsExtension<Topic> {

  @Query("{'_class': Topic}") override fun findAll(): List<Topic>

  @DeleteQuery("{'_class': Topic}") override fun deleteAll()
}
