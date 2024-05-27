/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.asMilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.assembler.MilestonePayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.service.MilestoneQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service.RelationQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class MilestoneGraphQlController(
    private val milestonePayloadAssembler: MilestonePayloadAssembler,
    private val milestoneQueryService: MilestoneQueryService,
    private val relationQueryService: RelationQueryService
) {

  @BatchMapping("milestones")
  fun milestonesInProjects(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, List<MilestonePayloadV1>> {
    val milestones =
        milestoneQueryService
            .findAllByProjectsAndDeletedFalse(projects.map { it.id.asProjectId() })
            .groupBy { it.project }

    return projects.associateWith {
      milestones[it.id.asProjectId()]?.map { milestonePayloadAssembler.assemble(it, null) }
          ?: emptyList()
    }
  }

  @BatchMapping("predecessorMilestones")
  fun milestonePredecessorsInMilestones(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<MilestonePayloadV1>> {
    val relations =
        relationQueryService.findAllPredecessorsOfMilestonesOfTypeAndDeletedFalse(
            milestones.map { it.id.asMilestoneId() }, MILESTONE.name)

    val milestoneIdsByMilestoneId =
        relations
            .map {
              Triple(
                  it.target.identifier.asMilestoneId(),
                  it.source.identifier.asMilestoneId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findMilestonesAndAssembleResponse(milestoneIdsByMilestoneId, milestones)
  }

  @BatchMapping("successorMilestones")
  fun milestoneSuccessorsInMilestones(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<MilestonePayloadV1>> {
    val relations =
        relationQueryService.findAllSuccessorsOfMilestoneOfTypeAndDeletedFalse(
            milestones.map { it.id.asMilestoneId() }, MILESTONE.name)

    val milestoneIdsByMilestoneId =
        relations
            .map {
              Triple(
                  it.source.identifier.asMilestoneId(),
                  it.target.identifier.asMilestoneId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findMilestonesAndAssembleResponse(milestoneIdsByMilestoneId, milestones)
  }

  @BatchMapping("predecessorMilestones")
  fun milestonePredecessorsInTasks(
      tasks: List<TaskPayloadV1>
  ): Map<TaskPayloadV1, List<MilestonePayloadV1>> {
    val relations =
        relationQueryService.findAllPredecessorsOfTasksOfTypeAndDeletedFalse(
            tasks.map { it.id.asTaskId() }, MILESTONE.name)

    val milestoneIdsByTaskId =
        relations
            .map {
              Triple(
                  it.target.identifier.asTaskId(),
                  it.source.identifier.asMilestoneId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findMilestonesForTasksAndAssembleResponse(milestoneIdsByTaskId, tasks)
  }

  @BatchMapping("successorMilestones")
  fun milestoneSuccessorsInTasks(
      tasks: List<TaskPayloadV1>
  ): Map<TaskPayloadV1, List<MilestonePayloadV1>> {
    val relations =
        relationQueryService.findAllSuccessorsOfTasksOfTypeAndDeletedFalse(
            tasks.map { it.id.asTaskId() }, MILESTONE.name)

    val milestoneIdsByTaskId =
        relations
            .map {
              Triple(
                  it.source.identifier.asTaskId(),
                  it.target.identifier.asMilestoneId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findMilestonesForTasksAndAssembleResponse(milestoneIdsByTaskId, tasks)
  }

  @BatchMapping("milestones")
  fun milestonesInTasks(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, List<MilestonePayloadV1>> {
    val taskIds = tasks.map { it.id.asTaskId() }

    val successorRelations =
        relationQueryService
            .findAllSuccessorsOfTasksOfTypeAndDeletedFalse(
                tasks.map { it.id.asTaskId() }, MILESTONE.name)
            .map { Pair(it.source.identifier.asTaskId(), it.target.identifier.asMilestoneId()) }

    val partOfRelations =
        relationQueryService.findAllParentMilestonesAndDeletedFalse(taskIds).map {
          Pair(it.source.identifier.asTaskId(), it.target.identifier.asMilestoneId())
        }

    val milestoneIdsByTaskId =
        (successorRelations + partOfRelations)
            .distinct()
            .groupBy({ it.first }, { Pair(it.second, null) })

    return findMilestonesForTasksAndAssembleResponse(milestoneIdsByTaskId, tasks)
  }

  private fun findMilestonesAndAssembleResponse(
      milestoneIdsByMilestoneId: Map<MilestoneId, List<Pair<MilestoneId, Boolean?>>>,
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<MilestonePayloadV1>> {
    val referencedMilestones =
        milestoneQueryService
            .findAllByIdentifiersAndDeletedFalse(
                milestoneIdsByMilestoneId.values.map { it.map { it.first } }.flatten().distinct())
            .associateBy { it.identifier }

    return milestones.associateWith {
      milestoneIdsByMilestoneId[it.id.asMilestoneId()]?.mapNotNull { idWithCriticality ->
        referencedMilestones[idWithCriticality.first]?.let {
          milestonePayloadAssembler.assemble(it, idWithCriticality.second)
        }
      }
          ?: emptyList()
    }
  }

  private fun findMilestonesForTasksAndAssembleResponse(
      milestoneIdsByTaskId: Map<TaskId, List<Pair<MilestoneId, Boolean?>>>,
      tasks: List<TaskPayloadV1>
  ): Map<TaskPayloadV1, List<MilestonePayloadV1>> {
    val milestones =
        milestoneQueryService
            .findAllByIdentifiersAndDeletedFalse(
                milestoneIdsByTaskId.values.map { it.map { it.first } }.flatten().distinct())
            .associateBy { it.identifier }

    return tasks.associateWith {
      milestoneIdsByTaskId[it.id.asTaskId()]?.mapNotNull { idWithCriticality ->
        milestones[idWithCriticality.first]?.let {
          milestonePayloadAssembler.assemble(it, idWithCriticality.second)
        }
      }
          ?: emptyList()
    }
  }
}
