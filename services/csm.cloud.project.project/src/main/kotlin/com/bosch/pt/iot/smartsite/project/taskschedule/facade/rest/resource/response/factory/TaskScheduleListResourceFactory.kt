/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleListResource
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import org.springframework.stereotype.Component

@Component
open class TaskScheduleListResourceFactory(
    private val taskScheduleListResourceFactoryHelper: TaskScheduleListResourceFactoryHelper
) {

  open fun build(schedules: Collection<TaskScheduleWithDayCardsDto>) =
      TaskScheduleListResource(taskScheduleListResourceFactoryHelper.build(schedules))

  open fun buildBatch(schedules: Collection<TaskScheduleWithDayCardsDto>) =
      BatchResponseResource(items = taskScheduleListResourceFactoryHelper.build(schedules))
}
