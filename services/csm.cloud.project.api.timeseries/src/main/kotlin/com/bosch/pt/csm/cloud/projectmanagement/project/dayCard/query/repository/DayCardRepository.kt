/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import org.springframework.data.mongodb.repository.MongoRepository

interface DayCardRepository : MongoRepository<DayCard, DayCardId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: DayCardId): DayCard?

  fun findAllByTaskInAndDeletedFalse(taskIds: List<TaskId>): List<DayCard>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<DayCard>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<DayCard>
}
