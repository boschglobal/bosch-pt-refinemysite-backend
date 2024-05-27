/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MilestoneListRepository :
    JpaRepository<MilestoneList, Long>, MilestoneListRepositoryExtension {

  @EntityGraph(
      attributePaths = ["project", "milestones", "workArea", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: MilestoneListId): MilestoneList?

  fun findOneByIdentifier(identifier: MilestoneListId): MilestoneList?

  @Query("select ml.id from MilestoneList ml where ml.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: MilestoneListId): Long?

  @Query(
      "select ms.identifier from MilestoneList ml " +
          "left join ml.milestones ms " +
          "where ml.identifier = :listIdentifier")
  fun findMilestoneIdentifiersByMilestoneListIdentifier(
      @Param("listIdentifier") listIdentifier: MilestoneListId
  ): List<MilestoneId>

  @Query(
      "select ml.project.identifier from MilestoneList ml " +
          "where ml.identifier = :milestoneListIdentifier")
  fun findProjectIdentifierByIdentifier(
      @Param("milestoneListIdentifier") milestoneListIdentifier: MilestoneListId
  ): ProjectId?

  @EntityGraph(
      attributePaths =
          [
              "project",
              "milestones.workArea",
              "milestones.craft",
              "workArea",
              "createdBy",
              "lastModifiedBy"])
  fun findAllWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): List<MilestoneList>

  fun findAllByProjectId(projectId: Long): List<MilestoneList>
}
