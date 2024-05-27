/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.model.TaskAttachment
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskAttachmentRepository :
    MongoRepository<TaskAttachment, AggregateIdentifier>,
    ShardedSaveOperation<TaskAttachment, AggregateIdentifier>,
    TaskAttachmentRepositoryExtension
