/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.repository

import com.bosch.pt.iot.smartsite.project.importer.model.ProjectImport
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectImportRepository : JpaRepository<ProjectImport, Long> {

  fun findOneByProjectIdentifier(projectIdentifier: ProjectId): ProjectImport?

  fun existsByJobId(jobId: UUID): Boolean

  fun deleteByJobId(jobId: UUID)

  fun deleteByProjectIdentifier(projectIdentifier: ProjectId)
}
