/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.assembler

import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleDto
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto.Companion.convertToMap
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleResource

object SaveTaskScheduleDtoAssembler {

  fun assemble(createTaskScheduleResource: CreateTaskScheduleResource): SaveTaskScheduleDto =
      SaveTaskScheduleDto(createTaskScheduleResource.start, createTaskScheduleResource.end, null)

  fun assemble(updateTaskScheduleResource: UpdateTaskScheduleResource): SaveTaskScheduleDto =
      SaveTaskScheduleDto(
          updateTaskScheduleResource.start,
          updateTaskScheduleResource.end,
          convertToMap(updateTaskScheduleResource.slots))
}
