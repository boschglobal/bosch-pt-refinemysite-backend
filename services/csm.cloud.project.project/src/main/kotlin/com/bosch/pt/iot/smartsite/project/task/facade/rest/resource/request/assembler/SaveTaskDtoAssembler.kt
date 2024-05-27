/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.assembler

import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.dto.SaveTaskDto

object SaveTaskDtoAssembler {

  fun assemble(saveTaskResource: SaveTaskResource): SaveTaskDto =
      SaveTaskDto(
          saveTaskResource.name,
          saveTaskResource.description,
          saveTaskResource.location,
          saveTaskResource.status,
          saveTaskResource.projectCraftId,
          saveTaskResource.assigneeId,
          saveTaskResource.workAreaId)
}
