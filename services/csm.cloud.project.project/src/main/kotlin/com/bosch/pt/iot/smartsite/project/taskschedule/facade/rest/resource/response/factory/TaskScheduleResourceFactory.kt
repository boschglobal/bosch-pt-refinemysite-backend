/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import org.springframework.stereotype.Component

@Component
open class TaskScheduleResourceFactory(
    private val taskScheduleListResourceFactoryHelper: TaskScheduleListResourceFactoryHelper
) {

  open fun build(schedule: TaskScheduleWithDayCardsDto): TaskScheduleResource =
      taskScheduleListResourceFactoryHelper.build(setOf(schedule)).first()
}
