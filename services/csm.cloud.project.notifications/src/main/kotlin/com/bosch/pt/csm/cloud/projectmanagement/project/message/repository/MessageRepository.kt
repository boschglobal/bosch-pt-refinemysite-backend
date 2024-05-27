/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import org.springframework.data.mongodb.repository.MongoRepository

interface MessageRepository :
    MongoRepository<Message, AggregateIdentifier>,
    ShardedSaveOperation<Message, AggregateIdentifier>,
    MessageRepositoryExtension
