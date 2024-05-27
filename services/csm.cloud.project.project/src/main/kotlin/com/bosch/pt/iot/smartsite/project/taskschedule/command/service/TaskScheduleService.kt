/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.command.service

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskScheduleService(private val taskScheduleRepository: TaskScheduleRepository) {

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findByTaskIdentifier(taskIdentifier: TaskId): TaskSchedule =
      taskScheduleRepository.findOneByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findWithDetailsByTaskIdentifier(taskIdentifier: TaskId): TaskSchedule =
      taskScheduleRepository.findWithDetailsByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findWithDetailsByTaskIdentifiers(
      taskIdentifiers: Collection<TaskId>
  ): List<TaskSchedule> = taskScheduleRepository.findWithDetailsByTaskIdentifierIn(taskIdentifiers)

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun findTaskScheduleIdentifierByTaskIdentifier(taskIdentifier: TaskId): TaskScheduleId =
      taskScheduleRepository.findIdentifierByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())
}
