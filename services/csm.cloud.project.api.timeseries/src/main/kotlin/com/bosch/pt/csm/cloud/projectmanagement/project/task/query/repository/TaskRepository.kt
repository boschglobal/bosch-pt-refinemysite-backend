/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskRepository : MongoRepository<Task, TaskId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: TaskId): Task?

  fun findAllByIdentifierInAndDeletedFalse(taskIds: List<TaskId>): List<Task>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<Task>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<Task>
}
