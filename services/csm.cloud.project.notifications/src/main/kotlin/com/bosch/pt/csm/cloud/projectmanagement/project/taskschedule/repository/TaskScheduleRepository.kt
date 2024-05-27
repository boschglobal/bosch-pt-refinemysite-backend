/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskScheduleRepository :
    MongoRepository<TaskSchedule, AggregateIdentifier>,
    ShardedSaveOperation<TaskSchedule, AggregateIdentifier>,
    TaskScheduleRepositoryExtension
