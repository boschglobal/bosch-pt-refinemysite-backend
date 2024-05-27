/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import org.springframework.data.mongodb.repository.MongoRepository

interface WorkAreaRepository : MongoRepository<WorkArea, WorkAreaId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: WorkAreaId): WorkArea?

  fun findAllByIdentifierInAndDeletedFalse(identifiers: List<WorkAreaId>): List<WorkArea>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<WorkArea>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<WorkArea>
}
