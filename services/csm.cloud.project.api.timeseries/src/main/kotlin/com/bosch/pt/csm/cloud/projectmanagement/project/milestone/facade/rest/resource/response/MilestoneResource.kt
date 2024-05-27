/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import java.time.LocalDate

data class MilestoneResource(
    val id: MilestoneId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val type: String,
    val date: LocalDate,
    val global: Boolean,
    val description: String?,
    val craft: ProjectCraftId? = null,
    val workArea: WorkAreaId? = null,
    val deleted: Boolean = false,
    val eventTimestamp: Long
)
