/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.service

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountAggregation
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusCountProjection
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.RemoveDayCardsFromTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard.RemoveDayCardFromTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DayCardService(
    private val removeDayCardFromTaskScheduleCommandHandler:
        RemoveDayCardFromTaskScheduleCommandHandler,
    private val dayCardRepository: DayCardRepository
) {

  open fun removeDayCardsFromTaskSchedule(
      identifier: DayCardId,
      taskScheduleIdentifier: TaskScheduleId,
      scheduleETag: ETag
  ) {
    removeDayCardFromTaskScheduleCommandHandler.handle(
        RemoveDayCardsFromTaskScheduleCommand(identifier, taskScheduleIdentifier, scheduleETag))
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun getStatusCountAggregation(
      taskIdentifiers: Collection<TaskId>
  ): Map<TaskId, DayCardStatusCountAggregation> {
    val aggregationMap: MutableMap<TaskId, DayCardStatusCountAggregation> = HashMap()
    dayCardRepository.countStatusGroupedByTaskIdentifier(taskIdentifiers).forEach {
        projection: DayCardStatusCountProjection ->
      aggregationMap.compute(projection.taskIdentifier) {
          _: TaskId?,
          v: DayCardStatusCountAggregation? ->
        if (v == null) {
          return@compute DayCardStatusCountAggregation(projection.status, projection.count)
        } else {
          v.countByStatus[projection.status] = projection.count
          return@compute v
        }
      }
    }
    return aggregationMap
  }
}
