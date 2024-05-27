/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.query

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithoutDayCardsDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskScheduleQueryService(private val taskScheduleRepository: TaskScheduleRepository) {

  @Trace
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProjects(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun find(
      identifier: TaskScheduleId,
      projectIdentifier: ProjectId
  ): TaskScheduleWithDayCardsDto {
    val scheduleWithoutDayCards =
        taskScheduleRepository.findEntityByIdentifierAndProjectIdentifier(
            identifier, projectIdentifier)
            ?: throw AggregateNotFoundException(
                TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

    val taskIdentifier = scheduleWithoutDayCards.taskIdentifier
    val scheduleSlotsWithDayCards =
        taskScheduleRepository.findAllDayCardsByTaskIdentifierIn(setOf(taskIdentifier))

    return TaskScheduleWithDayCardsDto(scheduleWithoutDayCards, scheduleSlotsWithDayCards)
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findTaskScheduleWithDayCardsDtoByTaskIdentifier(
      taskIdentifier: TaskId
  ): TaskScheduleWithDayCardsDto {
    val scheduleWithoutDayCards =
        taskScheduleRepository.findTaskScheduleWithoutDayCardsDtoByTaskIdentifier(taskIdentifier)
            ?: throw AggregateNotFoundException(
                TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

    val scheduleSlotsWithDayCards =
        taskScheduleRepository.findAllDayCardsByTaskIdentifierIn(setOf(taskIdentifier))

    return TaskScheduleWithDayCardsDto(scheduleWithoutDayCards, scheduleSlotsWithDayCards)
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findByTaskIdentifiers(
      taskIdentifiers: Collection<TaskId>
  ): Collection<TaskScheduleWithDayCardsDto> {
    val schedules =
        taskScheduleRepository.findTaskScheduleWithoutDayCardsDtosByTaskIdentifiers(taskIdentifiers)

    val scheduleToSlotsWithDayCardMap =
        taskScheduleRepository.findAllDayCardsByTaskIdentifierIn(taskIdentifiers).groupBy {
          it.identifier
        }

    return schedules.map { simpleScheduleDto: TaskScheduleWithoutDayCardsDto ->
      TaskScheduleWithDayCardsDto(
          simpleScheduleDto,
          scheduleToSlotsWithDayCardMap[simpleScheduleDto.identifier] ?: emptyList())
    }
  }

  @Trace
  @PreAuthorize(
      "@taskScheduleAuthorizationComponent.hasReadPermissionOnTaskSchedules(#taskScheduleIdentifiers)")
  @Transactional(readOnly = true)
  open fun findByTaskScheduleIdentifiers(
      taskScheduleIdentifiers: Collection<TaskScheduleId>
  ): Collection<TaskScheduleWithDayCardsDto> {
    val schedules =
        taskScheduleRepository.findTaskScheduleWithoutDayCardsDtosByIdentifiers(
            taskScheduleIdentifiers)

    val scheduleToSlotsWithDayCardMap =
        taskScheduleRepository.findAllDayCardsByIdentifierIn(taskScheduleIdentifiers).groupBy {
          it.identifier
        }

    return schedules.map { simpleScheduleDto: TaskScheduleWithoutDayCardsDto ->
      TaskScheduleWithDayCardsDto(
          simpleScheduleDto,
          scheduleToSlotsWithDayCardMap[simpleScheduleDto.identifier] ?: emptyList())
    }
  }
}
