/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import org.springframework.stereotype.Component

@Component
class ProjectListResourceAssembler(private val projectResourceAssembler: ProjectResourceAssembler) {

  fun assemble(projects: List<Project>, latestOnly: Boolean): ProjectListResource =
      ProjectListResource(
          projects
              .flatMap { projectResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
