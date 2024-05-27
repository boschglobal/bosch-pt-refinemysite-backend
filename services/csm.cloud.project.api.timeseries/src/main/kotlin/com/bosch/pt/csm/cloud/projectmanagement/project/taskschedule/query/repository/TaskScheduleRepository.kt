/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskScheduleRepository : MongoRepository<TaskSchedule, TaskScheduleId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: TaskScheduleId): TaskSchedule?

  fun findAllByTaskIn(taskIds: List<TaskId>): List<TaskSchedule>

  fun findAllByTaskInAndDeletedFalse(taskIds: List<TaskId>): List<TaskSchedule>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<TaskSchedule>
}
