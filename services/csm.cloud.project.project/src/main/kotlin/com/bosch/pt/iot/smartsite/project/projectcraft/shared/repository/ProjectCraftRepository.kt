/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.PopulateExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.UseExistenceCache
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectCraftRepository : JpaRepository<ProjectCraft, Long> {

  fun findOneByIdentifier(projectCraftIdentifier: ProjectCraftId): ProjectCraft?

  fun existsByNameIgnoreCaseAndProjectIdentifier(name: String, projectId: ProjectId): Boolean

  fun countByProjectIdentifier(projectIdentifier: ProjectId): Long

  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(projectCraftIdentifier: ProjectCraftId): ProjectCraft?

  @Query("select pc.id from ProjectCraft pc where pc.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: ProjectCraftId): Long?

  @Query(
      "select pc.project.identifier from ProjectCraft pc where pc.identifier = :projectCraftIdentifier")
  fun findProjectIdentifierByIdentifier(
      @Param("projectCraftIdentifier") projectCraftIdentifier: ProjectCraftId
  ): ProjectId?

  @Query("select c from ProjectCraft c join c.project p where p.id = :id")
  fun findAllByProjectId(@Param("id") projectId: Long): List<ProjectCraft>

  @Query("select craft.identifier from ProjectCraft craft where craft.identifier in :identifiers")
  fun validateExistingIdentifiersFor(
      @Param("identifiers") identifiers: Collection<ProjectCraftId>
  ): Collection<ProjectCraftId>

  // Queries that implement ExistenceCache to improve performance on exists queries
  @PopulateExistenceCache(cacheName = "projectCraft", keyFromResult = ["identifier"])
  @EntityGraph(attributePaths = ["project", "createdBy", "lastModifiedBy"])
  fun findAllByIdentifierIn(identifiers: Collection<ProjectCraftId>): List<ProjectCraft>

  @PopulateExistenceCache(
      cacheName = "projectCraft", keyFromResult = ["identifier", "project.identifier"])
  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<ProjectCraft>

  @UseExistenceCache(
      cacheName = "projectCraft",
      keyFromParameters = ["projectCraftIdentifier", "projectIdentifier"])
  fun existsByIdentifierAndProjectIdentifier(
      projectCraftIdentifier: ProjectCraftId,
      projectIdentifier: ProjectId
  ): Boolean

  @EvictExistenceCache override fun delete(projectCraft: ProjectCraft)

  @EvictExistenceCache override fun deleteAll(projectCrafts: MutableIterable<ProjectCraft>)
}
