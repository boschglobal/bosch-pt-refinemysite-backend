/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectCraftListRepository : JpaRepository<ProjectCraftList, Long> {

  fun findOneByIdentifier(identifier: ProjectCraftListId): ProjectCraftList?

  fun findAllByProjectId(projectId: Long): List<ProjectCraftList>

  @EntityGraph(attributePaths = ["project", "projectCrafts"])
  fun findOneWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): ProjectCraftList?

  @Query("select pcl.id from ProjectCraftList pcl where pcl.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: ProjectCraftListId): Long?

  // This function is for test use only
  @EntityGraph(attributePaths = ["project", "projectCrafts"])
  fun findOneWithDetailsByIdentifier(identifier: ProjectCraftListId): ProjectCraftList?
}
