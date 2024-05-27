/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.iot.smartsite.project.milestone.command.api.UpdateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.Companion.ENUM_VALUES
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate

data class UpdateMilestoneResource(

    // Name
    val name: String,

    // Type
    @StringEnumeration(enumClass = MilestoneTypeEnum::class, enumValues = ENUM_VALUES)
    val type: MilestoneTypeEnum,

    // Date
    val date: LocalDate,

    // Header
    val header: Boolean,

    // Description
    val description: String? = null,

    // craft
    val craftId: ProjectCraftId? = null,

    // Work area
    val workAreaId: WorkAreaId? = null,

    // position in milestone list where milestone shall be inserted / moved to
    val position: Int? = null
) {

  fun toCommand(identifier: MilestoneId, eTag: ETag) =
      UpdateMilestoneCommand(
          identifier = identifier,
          version = eTag.toVersion(),
          name = name,
          type = type,
          date = date,
          header = header,
          description = description,
          craftRef = craftId,
          workAreaRef = workAreaId,
          position = position)

  companion object {
    fun new() =
        UpdateMilestoneResource(
            name = "Dummy",
            type = MilestoneTypeEnum.PROJECT,
            date = LocalDate.now(),
            header = true,
            craftId = ProjectCraftId())
  }
}
