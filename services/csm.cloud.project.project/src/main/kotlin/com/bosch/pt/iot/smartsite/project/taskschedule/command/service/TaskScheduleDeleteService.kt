/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.service

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.daycard.command.service.DayCardDeleteService
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskScheduleDeleteService {
  @Autowired protected lateinit var dayCardRepository: DayCardRepository
  @Autowired protected lateinit var taskScheduleRepository: TaskScheduleRepository
  @Autowired protected lateinit var dayCardDeleteService: DayCardDeleteService

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deletePartitioned(taskIds: List<Long>) {
    val scheduleIds = taskScheduleRepository.getIdsByTaskIdsPartitioned(taskIds)
    taskScheduleRepository.deleteScheduleSlotsPartitioned(scheduleIds)
    dayCardDeleteService.deletePartitioned(scheduleIds)
    taskScheduleRepository.deletePartitioned(scheduleIds)
  }
}
