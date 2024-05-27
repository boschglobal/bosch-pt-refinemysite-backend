/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone.Companion.MAX_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.Companion.ENUM_VALUES
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateMilestoneResource(

    // Name
    @field:Size(min = 1, max = MAX_NAME_LENGTH) val name: String,

    // Type
    @StringEnumeration(enumClass = MilestoneTypeEnum::class, enumValues = ENUM_VALUES)
    val type: MilestoneTypeEnum,

    // Date
    val date: LocalDate,

    // Header
    val header: Boolean,

    // Project
    val projectId: ProjectId,

    // Description
    @field:Size(min = 1, max = MAX_DESCRIPTION_LENGTH) val description: String? = null,

    // craft
    val craftId: ProjectCraftId? = null,

    // Work area
    val workAreaId: WorkAreaId? = null,

    // position in milestone list where milestone shall be inserted
    val position: Int = 0
) {

  fun toCommand(identifier: MilestoneId?) =
      CreateMilestoneCommand(
          identifier = identifier ?: MilestoneId(),
          projectRef = projectId,
          name = name,
          type = type,
          date = date,
          header = header,
          description = description,
          craftRef = craftId,
          workAreaRef = workAreaId,
          position = position)
}
