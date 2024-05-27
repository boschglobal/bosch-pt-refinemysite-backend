/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskcopy.command.api.TaskCopyCommand
import com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto.OverridableTaskParametersDto
import jakarta.validation.Valid

data class CopyTaskResource(
    val id: TaskId,
    val shiftDays: Long,
    val includeDayCards: Boolean = true,
    @Valid val parametersOverride: OverridableTaskParametersDto? = null
) {

  fun toCommand() =
      TaskCopyCommand(
          copyFromIdentifier = id,
          shiftDays = shiftDays,
          includeDayCards = includeDayCards,
          parametersOverride = parametersOverride)
}
