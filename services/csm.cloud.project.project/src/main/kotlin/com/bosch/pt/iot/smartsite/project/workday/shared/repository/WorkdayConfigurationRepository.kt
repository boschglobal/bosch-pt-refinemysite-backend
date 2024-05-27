/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.shared.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WorkdayConfigurationRepository : JpaRepository<WorkdayConfiguration, Long> {
  @Query(
      "select workdayConfiguration.identifier from WorkdayConfiguration workdayConfiguration " +
          "where workdayConfiguration.project.identifier = :projectIdentifier")
  fun findIdentifierByProjectIdentifier(
      @Param("projectIdentifier") projectIdentifier: ProjectId
  ): WorkdayConfigurationId?

  /*
   * Note: The workingDays and the holidays should be fetched eagerly, each in separate sub query to avoid a
   * cartesian product.
   *
   * Therefore, we
   * 1) must not mention workingDays and the holidays in attributePaths, and
   * 2) must use type = LOAD because a FETCH graph would override the eager loading of workingDays
   *    and holidays defined in entity.
   */
  @EntityGraph(attributePaths = ["project"], type = LOAD)
  fun findOneWithDetailsByIdentifier(
      workdayConfigurationId: WorkdayConfigurationId
  ): WorkdayConfiguration?

  /*
   * Note: see above.
   */
  @EntityGraph(attributePaths = ["project"], type = LOAD)
  fun findOneWithDetailsByProjectIdentifier(projectIdentifier: ProjectId): WorkdayConfiguration?
}
