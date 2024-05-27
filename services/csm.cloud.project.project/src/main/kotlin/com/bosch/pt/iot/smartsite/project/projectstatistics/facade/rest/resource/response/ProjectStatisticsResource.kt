/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL

class ProjectStatisticsResource(
    val draftTasks: Number,
    val openTasks: Number,
    val startedTasks: Number,
    val closedTasks: Number,
    val acceptedTasks: Number,
    val uncriticalTopics: Number,
    val criticalTopics: Number
) : AbstractResource() {

  constructor(
      taskStatistics: Map<TaskStatusEnum, Long>,
      topicStatistics: Map<TopicCriticalityEnum, Long>
  ) : this(
      draftTasks = taskStatistics.getOrDefault(DRAFT, 0L),
      openTasks = taskStatistics.getOrDefault(OPEN, 0L),
      startedTasks = taskStatistics.getOrDefault(STARTED, 0L),
      closedTasks = taskStatistics.getOrDefault(CLOSED, 0L),
      acceptedTasks = taskStatistics.getOrDefault(ACCEPTED, 0L),
      uncriticalTopics = topicStatistics.getOrDefault(UNCRITICAL, 0L),
      criticalTopics = topicStatistics.getOrDefault(CRITICAL, 0L))
}
