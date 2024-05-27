/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.domain.WorkAreaListId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaList
import org.springframework.data.mongodb.repository.MongoRepository

interface WorkAreaListRepository : MongoRepository<WorkAreaList, WorkAreaListId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: WorkAreaListId): WorkAreaList?

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<WorkAreaList>
}
