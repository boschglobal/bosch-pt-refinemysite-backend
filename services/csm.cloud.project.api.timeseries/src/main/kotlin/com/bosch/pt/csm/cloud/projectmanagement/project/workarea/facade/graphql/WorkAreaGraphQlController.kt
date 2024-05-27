/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.WorkAreaPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.assembler.WorkAreaPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.service.WorkAreaQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.WorkAreaListPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class WorkAreaGraphQlController(
    private val workAreaPayloadAssembler: WorkAreaPayloadAssembler,
    private val workAreaQueryService: WorkAreaQueryService
) {

  @BatchMapping(field = "workArea")
  fun milestoneWorkArea(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, WorkAreaPayloadV1?> {
    val workAreas =
        workAreaQueryService
            .findAllByIdentifiersAndDeletedFalse(milestones.mapNotNull { it.workAreaId })
            .associateBy { it.identifier }

    return milestones.associateWith { milestone ->
      milestone.workAreaId?.let { workAreas[it]?.let { workAreaPayloadAssembler.assemble(it) } }
    }
  }

  @BatchMapping(field = "workArea")
  fun taskWorkArea(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, WorkAreaPayloadV1?> {
    val workAreas =
        workAreaQueryService.findAllByProjectsAndDeletedFalse(tasks.map { it.projectId }).groupBy {
          it.project
        }

    return tasks.associateWith { task ->
      workAreas[task.projectId]
          ?.firstOrNull { workArea -> task.workAreaId == workArea.identifier }
          ?.let { workAreaPayloadAssembler.assemble(it) }
    }
  }

  @BatchMapping(field = "items")
  fun workAreasFromWorkAreaList(
      workAreaLists: List<WorkAreaListPayloadV1>
  ): Map<WorkAreaListPayloadV1, List<WorkAreaPayloadV1>> {
    val workAreas =
        workAreaQueryService
            .findAllByIdentifiersAndDeletedFalse(
                workAreaLists.flatMap { it.items.map { it.asWorkAreaId() } })
            .associateBy { it.identifier }

    return workAreaLists.associateWith {
      it.items.mapNotNull {
        workAreas[it.asWorkAreaId()]?.let { workAreaPayloadAssembler.assemble(it) }
      }
    }
  }
}
