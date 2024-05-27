/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ShardedSaveOperation
import com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.ProjectContextOperationsExtension
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface TaskScheduleRepository :
    MongoRepository<TaskSchedule, AggregateIdentifier>,
    ShardedSaveOperation<TaskSchedule, AggregateIdentifier>,
    ProjectContextOperationsExtension<TaskSchedule> {

  @Query("{'_class': TaskSchedule}") override fun findAll(): List<TaskSchedule>

  @DeleteQuery("{'_class': TaskSchedule}") override fun deleteAll()
}
