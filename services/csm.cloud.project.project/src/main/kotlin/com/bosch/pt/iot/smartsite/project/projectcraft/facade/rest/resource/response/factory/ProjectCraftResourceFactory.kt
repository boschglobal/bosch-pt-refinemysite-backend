/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import org.springframework.stereotype.Component

@Component
open class ProjectCraftResourceFactory(
    private val projectCraftResourceFactoryHelper: ProjectCraftResourceFactoryHelper
) {

  open fun build(projectCraft: ProjectCraft): ProjectCraftResource =
      projectCraftResourceFactoryHelper.build(setOf(projectCraft)).first()
}
