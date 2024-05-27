/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.workarea.command.api.ReorderWorkAreaListCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_POSITION_VALUE
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateWorkAreaListResource(
    val workAreaId: WorkAreaId,
    @field:Min(1) @field:Max(MAX_WORKAREA_POSITION_VALUE.toLong()) val position: Int
) {

  fun toCommand(eTag: ETag) =
      ReorderWorkAreaListCommand(
          workAreaRef = workAreaId, position = position, version = eTag.toVersion())
}
