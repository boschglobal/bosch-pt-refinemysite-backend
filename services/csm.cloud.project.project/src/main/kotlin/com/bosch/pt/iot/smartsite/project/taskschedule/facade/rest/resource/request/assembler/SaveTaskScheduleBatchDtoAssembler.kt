/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.assembler

import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleBatchDto
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto.Companion.convertToMap
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleBatchResource

object SaveTaskScheduleBatchDtoAssembler {

  fun assembleOfCreate(
      createTaskScheduleBatchResources: Collection<CreateTaskScheduleBatchResource>
  ): Collection<SaveTaskScheduleBatchDto> = createTaskScheduleBatchResources.map { assemble(it) }

  fun assembleOfUpdate(
      updateTaskScheduleBatchResources: Collection<UpdateTaskScheduleBatchResource>
  ): Collection<SaveTaskScheduleBatchDto> = updateTaskScheduleBatchResources.map { assemble(it) }

  private fun assemble(
      createTaskScheduleBatchResource: CreateTaskScheduleBatchResource
  ): SaveTaskScheduleBatchDto =
      SaveTaskScheduleBatchDto(
          null,
          null,
          createTaskScheduleBatchResource.taskId.asTaskId(),
          createTaskScheduleBatchResource.start,
          createTaskScheduleBatchResource.end,
          null)

  private fun assemble(
      updateTaskScheduleBatchResource: UpdateTaskScheduleBatchResource
  ): SaveTaskScheduleBatchDto =
      SaveTaskScheduleBatchDto(
          null,
          updateTaskScheduleBatchResource.version,
          updateTaskScheduleBatchResource.taskId.asTaskId(),
          updateTaskScheduleBatchResource.start,
          updateTaskScheduleBatchResource.end,
          convertToMap(updateTaskScheduleBatchResource.slots))
}
