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
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.UpdateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft.Companion.MAX_COLOR_LENGTH
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList.Companion.MAX_CRAFTS_ALLOWED
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

class SaveProjectCraftResource(
    @field:Size(min = 1, max = MAX_NAME_LENGTH) val name: String,
    @field:Size(min = 1, max = MAX_COLOR_LENGTH) val color: String,
    /** In the case of input position from the client the values start at 1. */
    @field:Min(1) @field:Max(MAX_CRAFTS_ALLOWED.toLong()) val position: Int? = null
) {

  fun toCommand(
      identifier: ProjectCraftId?,
      projectIdentifier: ProjectId,
      projectCraftListEtag: ETag
  ) =
      CreateProjectCraftCommand(
          projectIdentifier = projectIdentifier,
          identifier = identifier ?: ProjectCraftId(),
          name = name,
          color = color,
          projectCraftListVersion = projectCraftListEtag.toVersion(),
          position = position)

  fun toCommand(identifier: ProjectCraftId, eTag: ETag) =
      UpdateProjectCraftCommand(
          identifier = identifier, version = eTag.toVersion(), name = name, color = color)
}
