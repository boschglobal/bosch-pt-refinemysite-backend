/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId

data class ProjectCraftResource(
    val id: ProjectCraftId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val color: String,
    val deleted: Boolean,
    val eventTimestamp: Long
)
