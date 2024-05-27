/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.WorkAreaListPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.assembler.WorkAreaListPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.service.WorkAreaListQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class WorkAreaListGraphQlController(
    private val workAreaListPayloadAssembler: WorkAreaListPayloadAssembler,
    private val workAreaListQueryService: WorkAreaListQueryService
) {

  @BatchMapping(field = "workAreas")
  fun projectWorkAreas(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, WorkAreaListPayloadV1?> {
    val workAreaLists =
        workAreaListQueryService.findAllByProjectsAndDeletedFalse(
            projects.map { it.id.asProjectId() })

    return projects.associateWith {
      workAreaLists[it.id.asProjectId()]?.let { workAreaListPayloadAssembler.assemble(it) }
    }
  }
}
