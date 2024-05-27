/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.ProjectCraftController.Companion.CRAFTS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftListResource.Companion.LINK_CREATE
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftListResource.Companion.LINK_REORDER
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import org.springframework.stereotype.Component

@Component
open class ProjectCraftListResourceFactory(
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val projectCraftResourceFactoryHelper: ProjectCraftResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(
      projectCraftList: ProjectCraftList,
      projectIdentifier: ProjectId
  ): ProjectCraftListResource {
    val craftResources = projectCraftResourceFactoryHelper.build(projectCraftList.projectCrafts)

    return ProjectCraftListResource(craftResources, projectCraftList.version).apply {
      // add create link
      addAllIf(projectAuthorizationComponent.hasUpdatePermissionOnProject(projectIdentifier)) {
        listOf(
            linkFactory.linkTo(CRAFTS_ENDPOINT).withRel(LINK_CREATE),
            linkFactory.linkTo(CRAFTS_ENDPOINT).withRel(LINK_REORDER))
      }
    }
  }
}
