/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import org.springframework.hateoas.RepresentationModel

class ProjectCraftListResource(
    val projectCrafts: Collection<ProjectCraftResource>,
    val version: Long
) : RepresentationModel<AbstractResource>() {

  companion object {
    const val LINK_CREATE = "create"
    const val LINK_REORDER = "reorder"
  }
}
