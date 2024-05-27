/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.RfvPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.assembler.RfvPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.ProjectRfvs
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.service.RfvQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class RfvGraphQlController(
    private val rfvPayloadAssembler: RfvPayloadAssembler,
    private val rfvQueryService: RfvQueryService
) {

  @BatchMapping("rfvs")
  fun rfvsOfProjects(projects: List<ProjectPayloadV1>): Map<ProjectPayloadV1, List<RfvPayloadV1>> {
    val rfvs =
        rfvQueryService.findAllByProjectsAndDeletedFalse(projects.map { it.id.asProjectId() })

    return projects.associateWith { project ->
      val projectId = project.id.asProjectId()
      rfvPayloadAssembler.assemble(
          ProjectRfvs(projectId, rfvs[projectId]?.values?.toList() ?: emptyList()))
    }
  }
}
