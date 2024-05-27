/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskstatistics.repository

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatisticsEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskStatisticsRepository : JpaRepository<Task, Long> {

  /**
   * Returns the task statistics, number of critical and uncritical topics of the task, for the
   * specified task identifier
   *
   * @param taskIdentifiers the identifiers of the tasks
   * @return the task statistics
   */
  @Query(
      "SELECT new com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatisticsEntry" +
          "(COUNT(topic.criticality), topic.criticality, topic.task.identifier) " +
          "FROM Topic topic " +
          "WHERE topic.deleted = false AND topic.task.identifier IN :taskIdentifiers " +
          "GROUP BY topic.criticality, topic.task.identifier")
  fun getTaskStatistics(
      @Param("taskIdentifiers") taskIdentifiers: Set<TaskId>
  ): List<TaskStatisticsEntry>
}
