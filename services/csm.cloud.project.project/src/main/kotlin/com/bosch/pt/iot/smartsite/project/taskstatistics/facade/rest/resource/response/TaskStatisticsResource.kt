/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatistics

class TaskStatisticsResource(val uncriticalTopics: Long, val criticalTopics: Long) :
    AbstractResource() {

  constructor(
      taskStatistics: TaskStatistics?
  ) : this(taskStatistics?.uncriticalTopics ?: 0, taskStatistics?.criticalTopics ?: 0)
}
