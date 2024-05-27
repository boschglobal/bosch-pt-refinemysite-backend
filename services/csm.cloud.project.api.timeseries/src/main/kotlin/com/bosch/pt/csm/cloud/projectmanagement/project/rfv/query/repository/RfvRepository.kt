/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.RfvId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import org.springframework.data.mongodb.repository.MongoRepository

interface RfvRepository : MongoRepository<Rfv, RfvId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: RfvId): Rfv?

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<Rfv>

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<Rfv>
}
