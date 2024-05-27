/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.DayCardPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.assembler.DayCardPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.service.DayCardQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.service.RfvQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service.TaskScheduleQueryService
import java.time.LocalDate
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class DayCardGraphQlController(
    private val dayCardPayloadAssembler: DayCardPayloadAssembler,
    private val dayCardQueryService: DayCardQueryService,
    private val rfvQueryService: RfvQueryService,
    private val taskScheduleQueryService: TaskScheduleQueryService,
) {

  @BatchMapping
  fun date(dayCards: List<DayCardPayloadV1>): Map<DayCardPayloadV1, LocalDate?> {
    val schedules =
        taskScheduleQueryService
            .findAllByTasksAndDeletedFalse(dayCards.map { it.taskId }.distinct())
            .associateBy { it.task }

    return dayCards.associateWith { dayCard ->
      schedules[dayCard.taskId]?.slots?.firstOrNull { it.dayCardId.value == dayCard.id }?.date
    }
  }

  @BatchMapping
  @Suppress("UNCHECKED_CAST")
  fun dayCards(tasks: List<TaskPayloadV1>): Map<TaskPayloadV1, List<DayCardPayloadV1>> {
    val taskIds = tasks.map { it.id.asTaskId() }.distinct()
    val dayCards = dayCardQueryService.findAllByTasksAndDeletedFalse(taskIds).groupBy { it.task }

    val rfvs =
        rfvQueryService.findAllByProjectsAndDeletedFalse(tasks.map { it.projectId }.distinct())

    val schedulesByDayCardId =
        taskScheduleQueryService
            .findAllByTasksAndDeletedFalse(taskIds)
            .associateBy { it.task }
            .flatMap { scheduleByTask ->
              scheduleByTask.value.slots.map { it.dayCardId to scheduleByTask.value }
            }
            .associate { it.first to it.second }

    return tasks
        .associateWith { task ->
          dayCards[task.id.asTaskId()]?.mapNotNull {
            dayCardPayloadAssembler.assemble(it, rfvs, schedulesByDayCardId[it.identifier])
          }
        }
        .filter { it.value != null } as Map<TaskPayloadV1, List<DayCardPayloadV1>>
  }
}
