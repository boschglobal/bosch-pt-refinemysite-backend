/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_POSITION_VALUE
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MIN_WORKAREA_NAME_LENGTH
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class CreateWorkAreaResource(
    val projectId: ProjectId,
    @field:Size(min = MIN_WORKAREA_NAME_LENGTH, max = MAX_WORKAREA_NAME_LENGTH) val name: String,
    /** In the case of input position from the client the values start at 1. */
    @field:Min(1) @field:Max(MAX_WORKAREA_POSITION_VALUE.toLong()) val position: Int? = null
) {

  fun toCommand(identifier: WorkAreaId?, workAreaListVersion: Long) =
      CreateWorkAreaCommand(
          identifier = identifier ?: WorkAreaId(),
          projectRef = projectId,
          name = name,
          position = position,
          workAreaListVersion = workAreaListVersion)
}
