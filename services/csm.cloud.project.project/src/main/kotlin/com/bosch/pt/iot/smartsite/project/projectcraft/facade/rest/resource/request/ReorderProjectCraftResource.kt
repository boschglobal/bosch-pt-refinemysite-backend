/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.ReorderProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList.Companion.MAX_CRAFTS_ALLOWED
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class ReorderProjectCraftResource(
    val projectCraftId: ProjectCraftId,
    /** In the case of input position from the client the values start at 1. */
    @field:Min(1) @field:Max(MAX_CRAFTS_ALLOWED.toLong()) val position: Int
) {

  fun toCommand(projectIdentifier: ProjectId, eTag: ETag) =
      ReorderProjectCraftCommand(
          projectIdentifier = projectIdentifier,
          identifier = projectCraftId,
          projectCraftListVersion = eTag.toVersion(),
          position = position)
}
