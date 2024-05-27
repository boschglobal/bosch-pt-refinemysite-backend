/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.service

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class DayCardDeleteService {
  @Autowired protected lateinit var dayCardRepository: DayCardRepository

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deletePartitioned(taskScheduleIds: List<Long>) {
    val dayCardIds = dayCardRepository.getIdsByTaskScheduleIdsPartitioned(taskScheduleIds)
    dayCardRepository.deletePartitioned(dayCardIds)
  }
}
