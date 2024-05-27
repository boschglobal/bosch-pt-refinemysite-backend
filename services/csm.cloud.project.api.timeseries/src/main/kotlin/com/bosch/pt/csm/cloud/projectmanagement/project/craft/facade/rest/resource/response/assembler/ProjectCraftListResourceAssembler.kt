/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import org.springframework.stereotype.Component

@Component
class ProjectCraftListResourceAssembler(
    private val projectCraftResourceAssembler: ProjectCraftResourceAssembler
) {

  fun assemble(projectCrafts: List<ProjectCraft>, latestOnly: Boolean): ProjectCraftListResource =
      ProjectCraftListResource(
          projectCrafts
              .flatMap { projectCraftResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
