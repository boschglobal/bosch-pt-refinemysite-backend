/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.WorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import org.springframework.data.mongodb.repository.MongoRepository

interface WorkDayConfigurationRepository : MongoRepository<WorkDayConfiguration, WorkDayConfigurationId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(identifier: WorkDayConfigurationId): WorkDayConfiguration?

  fun findAllByProjectInAndDeletedFalse(projectIds: List<ProjectId>): List<WorkDayConfiguration>

  fun findAllByProjectIn(projectIds: List<ProjectId>): List<WorkDayConfiguration>
}
