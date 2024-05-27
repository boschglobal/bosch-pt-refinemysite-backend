/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskstatistics.model

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL

class TaskStatisticsEntry(
    val count: Long,
    val criticality: TopicCriticalityEnum,
    val taskIdentifier: TaskId
) {

  companion object {

    /**
     * Converts the given statistics entry from long to wide format, meaning task statistics entries
     * (of different criticality) pertaining to the same task are combined to a single instance of
     * [TaskStatistics].
     *
     * @param statisticsEntries the list of statistics entries
     * @return a mapping from tasks (identified by UUIDs) to associated task statistics.
     */
    fun toMap(statisticsEntries: List<TaskStatisticsEntry>): Map<TaskId, TaskStatistics> =
        statisticsEntries
            .groupingBy { it.taskIdentifier }
            .aggregate { _, accumulator: MutableMap<TopicCriticalityEnum, Long>?, element, first ->
              if (first) {
                HashMap<TopicCriticalityEnum, Long>().apply {
                  put(element.criticality, element.count)
                }
              } else {
                accumulator!!.apply {
                  this[element.criticality] = (this[element.criticality] ?: 0) + element.count
                }
              }
            }
            .mapValues {
              TaskStatistics(
                  uncriticalTopics = it.value[UNCRITICAL] ?: 0,
                  criticalTopics = it.value[CRITICAL] ?: 0)
            }
  }
}
