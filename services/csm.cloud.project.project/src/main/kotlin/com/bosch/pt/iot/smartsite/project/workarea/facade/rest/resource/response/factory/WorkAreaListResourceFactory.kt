/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.project.authorization.ProjectAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.WorkAreaController
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaListResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_CREATE
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_REORDER
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import org.springframework.stereotype.Component

@Component
open class WorkAreaListResourceFactory(
    private val workAreaResourceFactoryHelper: WorkAreaResourceFactoryHelper,
    private val projectAuthorizationComponent: ProjectAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(workAreaList: WorkAreaList, project: Project): WorkAreaListResource {
    val workAreaResources = workAreaResourceFactoryHelper.build(workAreaList.workAreas, project)
    return WorkAreaListResource(workAreaResources, workAreaList.version).apply {
      if (projectAuthorizationComponent.hasUpdatePermissionOnProject(project.identifier)) {
        // add create reference for work area
        add(linkFactory.linkTo(WorkAreaController.WORKAREAS_ENDPOINT).withRel(LINK_CREATE))
        add(linkFactory.linkTo(WorkAreaController.WORKAREAS_ENDPOINT).withRel(LINK_REORDER))
      }
    }
  }
}
