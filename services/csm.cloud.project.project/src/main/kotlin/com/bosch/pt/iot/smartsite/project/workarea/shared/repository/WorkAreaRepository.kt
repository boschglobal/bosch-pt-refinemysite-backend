/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.PopulateExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.existencecache.UseExistenceCache
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WorkAreaRepository : JpaRepository<WorkArea, Long> {

  @EntityGraph(attributePaths = ["project", "workAreaList.project", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: WorkAreaId): WorkArea?

  @Query("select wa.id from WorkArea wa where wa.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: WorkAreaId): Long?

  fun findAllByProjectId(projectId: Long): List<WorkArea>

  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<WorkArea>

  fun findWorkAreaByIdentifierAndProjectIdentifier(
      identifier: WorkAreaId,
      projectIdentifier: ProjectId
  ): WorkArea?

  fun countByParentAndProjectIdentifier(parentRef: WorkAreaId, projectIdentifier: ProjectId): Long

  @Query(
      "Select p.identifier from WorkArea w join w.project p where w.identifier = :workAreaIdentifier")
  fun findProjectIdentifierByWorkAreaIdentifier(
      @Param("workAreaIdentifier") workAreaIdentifier: WorkAreaId
  ): ProjectId?

  fun existsByNameIgnoreCaseAndProjectIdAndParent(
      name: String,
      projectId: Long,
      parent: WorkAreaId?
  ): Boolean

  @Query("select wa.identifier from WorkArea wa where wa.identifier in :identifiers")
  fun validateExistingIdentifiersFor(
      @Param("identifiers") identifiers: Collection<WorkAreaId>
  ): Collection<WorkAreaId>

  fun findOneByIdentifier(identifier: WorkAreaId): WorkArea?

  fun countByProjectIdentifier(projectIdentifier: ProjectId): Long

  // Queries that implement ExistenceCache to improve performance on exists queries
  @PopulateExistenceCache(
      cacheName = "workArea", keyFromResult = ["identifier", "project.identifier"])
  fun findAllByIdentifierIn(identifiers: List<WorkAreaId>): List<WorkArea>

  @UseExistenceCache(
      cacheName = "workArea", keyFromParameters = ["identifier", "projectIdentifier"])
  fun existsByIdentifierAndProjectIdentifier(
      identifier: WorkAreaId,
      projectIdentifier: ProjectId
  ): Boolean

  @EvictExistenceCache override fun delete(workArea: WorkArea)

  @EvictExistenceCache override fun deleteAll(workAreas: MutableIterable<WorkArea>)
}
