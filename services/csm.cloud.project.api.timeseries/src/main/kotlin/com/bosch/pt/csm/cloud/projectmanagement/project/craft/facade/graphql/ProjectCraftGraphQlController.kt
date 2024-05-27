/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.ProjectCraftPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.assembler.ProjectCraftPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.service.ProjectCraftQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class ProjectCraftGraphQlController(
    private val projectCraftPayloadAssembler: ProjectCraftPayloadAssembler,
    private val projectCraftQueryService: ProjectCraftQueryService
) {

  @BatchMapping(field = "craft")
  fun milestoneCraft(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, ProjectCraftPayloadV1?> {
    val projectCrafts =
        projectCraftQueryService.findAllByIdentifiers(milestones.mapNotNull { it.craftId })

    return milestones.associateWith { milestone ->
      projectCrafts[milestone.craftId]?.let { projectCraftPayloadAssembler.assemble(it) }
    }
  }

  @BatchMapping(field = "craft")
  fun taskCraft(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, ProjectCraftPayloadV1?> {
    val projectCrafts =
        projectCraftQueryService
            .findAllByProjectsAndDeletedFalse(tasks.map { it.projectId }.distinct())
            .groupBy { it.project }

    return tasks.associateWith { task ->
      projectCrafts[task.projectId]
          ?.first { craft -> task.craftId == craft.identifier }
          ?.let { projectCraftPayloadAssembler.assemble(it) }
    }
  }

  @BatchMapping(field = "crafts")
  fun projectCrafts(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, List<ProjectCraftPayloadV1>> {
    val projectCrafts =
        projectCraftQueryService
            .findAllByProjectsAndDeletedFalse(projects.map { it.id.asProjectId() })
            .groupBy { it.project }

    return projects.associateWith {
      (projectCrafts[it.id.asProjectId()]?.map { projectCraftPayloadAssembler.assemble(it) }
          ?: emptyList())
    }
  }
}
