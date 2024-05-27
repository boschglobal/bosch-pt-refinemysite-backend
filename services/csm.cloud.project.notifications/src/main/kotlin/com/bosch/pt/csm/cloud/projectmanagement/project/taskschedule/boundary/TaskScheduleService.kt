/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskScheduleService(private val taskScheduleRepository: TaskScheduleRepository) {

  @Trace fun save(taskSchedule: TaskSchedule) = taskScheduleRepository.save(taskSchedule)

  @Trace
  fun deleteTaskSchedule(taskScheduleIdentifier: UUID, projectIdentifier: UUID) =
      taskScheduleRepository.deleteTaskSchedule(taskScheduleIdentifier, projectIdentifier)

  @Trace
  fun find(taskScheduleIdentifier: UUID, version: Long, projectIdentifier: UUID) =
      if (version < 0) null
      else taskScheduleRepository.find(taskScheduleIdentifier, version, projectIdentifier)
}
