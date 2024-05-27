/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.service

import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskScheduleService(private val taskScheduleRepository: TaskScheduleRepository) {

  @Trace fun save(taskSchedule: TaskSchedule) = taskScheduleRepository.save(taskSchedule)

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID) =
      taskScheduleRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      taskScheduleRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      taskScheduleRepository.deleteByVersion(identifier, version, projectIdentifier)
}
