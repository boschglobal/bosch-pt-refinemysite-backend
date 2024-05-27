/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface MilestoneRepository : MongoRepository<Milestone, MilestoneId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: MilestoneId): Milestone?

  fun findAllByIdentifierInAndDeletedFalse(identifiers: List<MilestoneId>): List<Milestone>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<Milestone>

  fun findAllByProjectInAndDeletedIsFalse(projectIds: List<ProjectId>): List<Milestone>
}
