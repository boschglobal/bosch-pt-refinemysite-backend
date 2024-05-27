/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft

class MilestoneProjectCraftReference(
    identifier: ProjectCraftId,
    displayName: String,
    val color: String
) : ResourceReference(identifier.toUuid(), displayName) {

  companion object {

    fun from(craft: ProjectCraft) =
        MilestoneProjectCraftReference(craft.identifier, craft.getDisplayName(), craft.color)
  }
}
