/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.TaskSchedulePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.springframework.stereotype.Component

@Component
class TaskSchedulePayloadAssembler {

  fun assemble(taskSchedule: TaskSchedule): TaskSchedulePayloadV1 =
      TaskSchedulePayloadMapper.INSTANCE.fromTaskSchedule(taskSchedule)
}
