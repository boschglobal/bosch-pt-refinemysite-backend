/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.WorkDayConfigurationPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.assembler.WorkDayConfigurationPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.service.WorkDayConfigurationQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class WorkDayConfigurationGraphQlController(
    private val workDayConfigurationPayloadAssembler: WorkDayConfigurationPayloadAssembler,
    private val workDayConfigurationQueryService: WorkDayConfigurationQueryService
) {

  @BatchMapping
  fun workDayConfiguration(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, WorkDayConfigurationPayloadV1?> {
    val workDayConfigurations =
        workDayConfigurationQueryService
            .findAllByProjectsAndDeletedFalse(projects.map { it.id.asProjectId() })
            .associateBy { it.project }

    return projects.associateWith { project ->
      workDayConfigurations[project.id.asProjectId()]?.let {
        workDayConfigurationPayloadAssembler.assemble(it)
      }
    }
  }
}
