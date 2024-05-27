/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.TaskConstraintSelectionId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskConstraintSelectionRepository :
    MongoRepository<TaskConstraintSelection, TaskConstraintSelectionId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: TaskConstraintSelectionId): TaskConstraintSelection?

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<TaskConstraintSelection>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<TaskConstraintSelection>

  fun findAllByTaskInAndDeletedFalse(projectIds: List<TaskId>): List<TaskConstraintSelection>
}
