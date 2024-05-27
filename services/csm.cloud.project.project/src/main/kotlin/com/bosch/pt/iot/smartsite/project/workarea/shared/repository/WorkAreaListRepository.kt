/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.shared.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WorkAreaListRepository : JpaRepository<WorkAreaList, Long> {

  @EntityGraph(
      attributePaths =
          [
              "project",
              "workAreas.createdBy",
              "workAreas.lastModifiedBy",
              "createdBy",
              "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: WorkAreaListId): WorkAreaList?

  @EntityGraph(attributePaths = ["project", "workAreas.createdBy", "workAreas.lastModifiedBy"])
  fun findOneWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): WorkAreaList?

  fun existsByProjectIdentifier(projectIdentifier: ProjectId): Boolean

  fun findOneByIdentifier(identifier: WorkAreaListId): WorkAreaList?

  @Query("select wl.id from WorkAreaList wl where wl.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: WorkAreaListId): Long?

  fun findAllByProjectId(projectId: Long): List<WorkAreaList>
}
