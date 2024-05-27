/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.TaskConstraintId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraint
import org.springframework.data.mongodb.repository.MongoRepository

interface TaskConstraintRepository : MongoRepository<TaskConstraint, TaskConstraintId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: TaskConstraintId): TaskConstraint?

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<TaskConstraint>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<TaskConstraint>
}
