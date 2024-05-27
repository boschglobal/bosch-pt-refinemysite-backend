/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ProjectCraftRepository : MongoRepository<ProjectCraft, ProjectCraftId> {

  fun deleteAllByProject(project: ProjectId)

  fun findOneByIdentifier(identifier: ProjectCraftId): ProjectCraft?

  fun findAllByIdentifierIn(identifiers: List<ProjectCraftId>): List<ProjectCraft>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<ProjectCraft>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<ProjectCraft>
}
