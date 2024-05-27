/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.model.Topic
import org.springframework.data.mongodb.repository.MongoRepository

interface TopicRepository :
    MongoRepository<Topic, AggregateIdentifier>,
    ShardedSaveOperation<Topic, AggregateIdentifier>,
    TopicRepositoryExtension
