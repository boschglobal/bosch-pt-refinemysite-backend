/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import org.springframework.stereotype.Component

@Component
class ProjectCraftResourceAssembler {

  fun assemble(projectCraft: ProjectCraft, latestOnly: Boolean): List<ProjectCraftResource> =
      if (latestOnly) {
        listOf(
            ProjectCraftResourceMapper.INSTANCE.fromProjectCraftVersion(
                projectCraft.history.last(), projectCraft.project, projectCraft.identifier))
      } else {
        projectCraft.history.map {
          ProjectCraftResourceMapper.INSTANCE.fromProjectCraftVersion(
              it, projectCraft.project, projectCraft.identifier)
        }
      }
}
