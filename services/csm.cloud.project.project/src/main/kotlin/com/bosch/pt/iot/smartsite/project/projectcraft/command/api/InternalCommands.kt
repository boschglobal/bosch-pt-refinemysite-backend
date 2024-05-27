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
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId

data class CreateProjectCraftListCommand(val projectIdentifier: ProjectId)

data class AddProjectCraftToListCommand(
    val identifier: ProjectCraftId,
    val projectCraftListIdentifier: ProjectCraftListId,
    val projectCraftListVersion: Long,
    val projectCraftListSize: Int,
    val position: Int?
)

data class ReorderProjectCraftListCommand(
    val identifier: ProjectCraftId,
    val projectCraftListIdentifier: ProjectCraftListId,
    val projectCraftListVersion: Long,
    val position: Int
)

data class RemoveProjectCraftFromListCommand(
    val identifier: ProjectCraftId,
    val projectCraftListIdentifier: ProjectCraftListId
)
