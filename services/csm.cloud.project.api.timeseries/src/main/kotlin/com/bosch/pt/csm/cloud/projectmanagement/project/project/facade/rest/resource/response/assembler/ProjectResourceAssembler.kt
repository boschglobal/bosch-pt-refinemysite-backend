/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import org.springframework.stereotype.Component

@Component
class ProjectResourceAssembler {

  fun assemble(project: Project, latestOnly: Boolean): List<ProjectResource> =
      if (latestOnly) {
        listOf(
            ProjectResourceMapper.INSTANCE.fromProject(
                project.history.last(), project.identifier, project.category?.key))
      } else {
        project.history.map {
          ProjectResourceMapper.INSTANCE.fromProject(it, project.identifier, project.category?.key)
        }
      }
}
