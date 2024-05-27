/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MilestoneRepository :
    JpaRepository<Milestone, Long>,
    MilestoneRepositoryExtension,
    JpaSpecificationExecutor<Milestone> {

  fun findOneByIdentifier(identifier: MilestoneId): Milestone?

  fun existsByIdentifierAndProjectIdentifier(
      identifier: MilestoneId,
      projectCraftIdentifier: ProjectId
  ): Boolean

  fun existsByIdentifierInAndProjectIdentifier(
      identifiers: Collection<MilestoneId>,
      projectCraftIdentifier: ProjectId
  ): Boolean

  @EntityGraph(attributePaths = ["project", "craft", "workArea", "createdBy", "lastModifiedBy"])
  fun findWithDetailsByIdentifier(identifier: MilestoneId): Milestone?

  @Query(
      "select milestone.project.identifier from Milestone milestone " +
          "where milestone.identifier = :milestoneIdentifier")
  fun findProjectIdentifierByIdentifier(
      @Param("milestoneIdentifier") milestoneIdentifier: MilestoneId?
  ): ProjectId?

  @EntityGraph(attributePaths = ["project", "craft", "workArea", "createdBy", "lastModifiedBy"])
  fun findAllWithDetailsByIdentifierIn(identifiers: Collection<MilestoneId>): List<Milestone>

  fun findAllByIdentifierIn(identifiers: Collection<MilestoneId>): List<Milestone>

  fun <T> findAllByIdentifierIn(identifiers: Collection<MilestoneId>, type: Class<T>): Collection<T>

  fun findAllByProjectId(projectId: Long): List<Milestone>

  fun findAllByProjectIdentifier(projectIdentifier: ProjectId): List<Milestone>

  @EntityGraph(attributePaths = ["project", "craft", "workArea", "createdBy", "lastModifiedBy"])
  fun findAllWithDetailsByIdentifierInAndProjectIdentifier(
      identifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId
  ): List<Milestone>

  fun <T> findAllByIdentifierInAndProjectIdentifier(
      identifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId,
      type: Class<T>
  ): List<T>

  @Query(
      "select milestone.identifier from Milestone milestone " +
          "where milestone.identifier in :identifiers " +
          "and milestone.project.identifier = :projectIdentifier")
  fun findMilestoneIdentifiersByIdentifierInAndProjectIdentifier(
      identifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId
  ): Set<MilestoneId>

  @Query(
      "select milestone.identifier from Milestone milestone " +
          "where milestone.type = :type " +
          "and milestone.identifier in :identifiers " +
          "and milestone.createdBy.identifier in :createdByIdentifiers")
  fun filterMilestonesCreatedByGivenSetOfUsers(
      @Param("type") type: MilestoneTypeEnum,
      @Param("identifiers") identifiers: Set<MilestoneId>,
      @Param("createdByIdentifiers") createdByIdentifiers: Set<UUID>
  ): Set<MilestoneId>

  fun existsByWorkAreaIdentifier(workAreaIdentifier: WorkAreaId): Boolean

  fun existsByCraftIdentifier(projectCraftIdentifier: ProjectCraftId): Boolean

  fun countByProjectIdentifier(projectIdentifier: ProjectId): Long
}
