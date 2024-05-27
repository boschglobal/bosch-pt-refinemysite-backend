/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.api

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId

data class CreateProjectCraftCommand(
    val projectIdentifier: ProjectId,
    val identifier: ProjectCraftId,
    val name: String,
    val color: String,
    val projectCraftListVersion: Long,
    val position: Int? = null
)

data class UpdateProjectCraftCommand(
    val identifier: ProjectCraftId,
    val version: Long,
    val name: String,
    val color: String
)

data class ReorderProjectCraftCommand(
    val projectIdentifier: ProjectId,
    val identifier: ProjectCraftId,
    val projectCraftListVersion: Long,
    val position: Int
)

data class DeleteProjectCraftCommand(
    val projectIdentifier: ProjectId,
    val identifier: ProjectCraftId,
    val version: Long
)
