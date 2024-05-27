/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.DayCardRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class DayCardService(private val dayCardRepository: DayCardRepository) {

  @Trace fun save(dayCard: DayCard): DayCard = dayCardRepository.save(dayCard)

  @Trace
  fun find(taskIdentifier: UUID, version: Long, projectIdentifier: UUID) =
      dayCardRepository.find(taskIdentifier, version, projectIdentifier)

  @Trace
  fun deleteDayCard(dayCardIdentifier: UUID, projectIdentifier: UUID) =
      dayCardRepository.deleteDayCard(dayCardIdentifier, projectIdentifier)
}
