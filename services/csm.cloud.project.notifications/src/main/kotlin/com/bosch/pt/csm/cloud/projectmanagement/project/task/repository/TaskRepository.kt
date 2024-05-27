/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation

import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskRepository :
    MongoRepository<Task, AggregateIdentifier>,
    ShardedSaveOperation<Task, AggregateIdentifier>,
    TaskRepositoryExtension
