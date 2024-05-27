/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.asMilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service.RelationQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.assembler.TaskPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.service.TaskQueryService
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class TaskGraphQlController(
    private val relationQueryService: RelationQueryService,
    private val taskPayloadAssembler: TaskPayloadAssembler,
    private val taskQueryService: TaskQueryService
) {

  @BatchMapping("tasks")
  fun tasksInProjects(
      projects: List<ProjectPayloadV1>
  ): Map<ProjectPayloadV1, List<TaskPayloadV1>> {
    val tasks =
        taskQueryService
            .findAllByProjectsAndDeletedFalse(projects.map { it.id.asProjectId() })
            .groupBy { it.project }

    return projects.associateWith {
      (tasks[it.id.asProjectId()]?.map { taskPayloadAssembler.assemble(it, null) } ?: emptyList())
    }
  }

  @BatchMapping("predecessorTasks")
  fun taskPredecessorsInMilestones(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<TaskPayloadV1>> {
    val relations =
        relationQueryService.findAllPredecessorsOfMilestonesOfTypeAndDeletedFalse(
            milestones.map { it.id.asMilestoneId() }, TASK.name)
    val referencedTaskIdsByTaskId =
        relations
            .map {
              Triple(
                  it.target.identifier.asMilestoneId(),
                  it.source.identifier.asTaskId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findTasksForMilestonesAndAssembleResponse(referencedTaskIdsByTaskId, milestones)
  }

  @BatchMapping("successorTasks")
  fun taskSuccessorsInMilestones(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<TaskPayloadV1>> {
    val relations =
        relationQueryService.findAllSuccessorsOfMilestoneOfTypeAndDeletedFalse(
            milestones.map { it.id.asMilestoneId() }, TASK.name)

    val referencingTaskIdsByTaskId =
        relations
            .map {
              Triple(
                  it.source.identifier.asMilestoneId(),
                  it.target.identifier.asTaskId(),
                  it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findTasksForMilestonesAndAssembleResponse(referencingTaskIdsByTaskId, milestones)
  }

  @BatchMapping("predecessorTasks")
  fun taskPredecessorsInTasks(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, List<TaskPayloadV1>> {
    val relations =
        relationQueryService.findAllPredecessorsOfTasksOfTypeAndDeletedFalse(
            tasks.map { it.id.asTaskId() }, TASK.name)
    val referencedTaskIdsByTaskId =
        relations
            .map {
              Triple(it.target.identifier.asTaskId(), it.source.identifier.asTaskId(), it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findTasksAndAssembleResponse(referencedTaskIdsByTaskId, tasks)
  }

  @BatchMapping("successorTasks")
  fun taskSuccessorsInTasks(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, List<TaskPayloadV1>> {
    val relations =
        relationQueryService.findAllSuccessorsOfTasksOfTypeAndDeletedFalse(
            tasks.map { it.id.asTaskId() }, TASK.name)

    val referencingTaskIdsByTaskId =
        relations
            .map {
              Triple(it.source.identifier.asTaskId(), it.target.identifier.asTaskId(), it.critical)
            }
            .groupBy({ it.first }, { Pair(it.second, it.third) })

    return findTasksAndAssembleResponse(referencingTaskIdsByTaskId, tasks)
  }

  @BatchMapping("requiredTasks")
  fun requiredTasksInMilestones(
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<TaskPayloadV1>> {
    val relations =
        relationQueryService.findAllNestedTasksAndDeletedFalse(
            milestones.map { it.id.asMilestoneId() })

    val taskIdsByMilestoneId =
        relations
            .map { Pair(it.target.identifier.asMilestoneId(), it.source.identifier.asTaskId()) }
            .groupBy({ it.first }, { Pair(it.second, null) })

    return findTasksForMilestonesAndAssembleResponse(taskIdsByMilestoneId, milestones)
  }

  private fun findTasksAndAssembleResponse(
      taskIdsByTaskId: Map<TaskId, List<Pair<TaskId, Boolean?>>>,
      tasks: List<TaskPayloadV1>
  ): Map<TaskPayloadV1, List<TaskPayloadV1>> {
    val referencedTasks =
        taskQueryService
            .findAllByIdentifiersAndDeletedFalse(
                taskIdsByTaskId.values.map { it.map { it.first } }.flatten().distinct())
            .associateBy { it.identifier }

    return tasks.associateWith {
      taskIdsByTaskId[it.id.asTaskId()]?.mapNotNull { idWithCriticality ->
        referencedTasks[idWithCriticality.first]?.let {
          taskPayloadAssembler.assemble(it, idWithCriticality.second)
        }
      }
          ?: emptyList()
    }
  }

  private fun findTasksForMilestonesAndAssembleResponse(
      taskIdsByMilestoneId: Map<MilestoneId, List<Pair<TaskId, Boolean?>>>,
      milestones: List<MilestonePayloadV1>
  ): Map<MilestonePayloadV1, List<TaskPayloadV1>> {
    val referencedTasks =
        taskQueryService
            .findAllByIdentifiersAndDeletedFalse(
                taskIdsByMilestoneId.values.map { it.map { it.first } }.flatten().distinct())
            .associateBy { it.identifier }

    return milestones.associateWith {
      taskIdsByMilestoneId[it.id.asMilestoneId()]?.mapNotNull { idWithCriticality ->
        referencedTasks[idWithCriticality.first]?.let {
          taskPayloadAssembler.assemble(it, idWithCriticality.second)
        }
      }
          ?: emptyList()
    }
  }
}
