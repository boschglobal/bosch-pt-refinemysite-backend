/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.workarea.command.api.UpdateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_NAME_LENGTH
import jakarta.validation.constraints.Size

data class UpdateWorkAreaResource(
    @field:Size(min = 1, max = MAX_WORKAREA_NAME_LENGTH) val name: String
) {

  fun toCommand(identifier: WorkAreaId, eTag: ETag) =
      UpdateWorkAreaCommand(identifier = identifier, version = eTag.toVersion(), name = name)
}
