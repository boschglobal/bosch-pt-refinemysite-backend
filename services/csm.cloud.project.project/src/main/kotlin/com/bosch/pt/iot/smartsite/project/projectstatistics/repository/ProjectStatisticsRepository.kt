/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TaskStatusStatisticsEntry
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TopicCriticalityStatisticsEntry
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectStatisticsRepository : JpaRepository<Project, UUID> {
  @Query(
      "SELECT NEW com.bosch.pt.iot.smartsite.project.projectstatistics.model.TaskStatusStatisticsEntry(" +
          "COUNT(t.status), t.status, t.project.identifier) " +
          "FROM Task t " +
          "WHERE t.deleted = false AND t.project.identifier IN :projectIdentifiers " +
          "GROUP BY t.status, t.project.identifier")
  fun getTaskStatistics(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>
  ): List<TaskStatusStatisticsEntry>

  @Query(
      "SELECT NEW com.bosch.pt.iot.smartsite.project.projectstatistics.model.TopicCriticalityStatisticsEntry(" +
          "COUNT(t.criticality), t.criticality, t.task.project.identifier) " +
          "FROM Topic t " +
          "WHERE t.task.deleted = false AND t.deleted = false AND t.task.project.identifier IN :projectIdentifiers " +
          "GROUP BY t.criticality, t.task.project.identifier")
  fun getTopicStatistics(
      @Param("projectIdentifiers") projectIdentifiers: Set<ProjectId>
  ): List<TopicCriticalityStatisticsEntry>
}
