/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.service

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository.DayCardRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class DayCardService(private val dayCardRepository: DayCardRepository) {

  @Trace fun save(dayCard: DayCard) = dayCardRepository.save(dayCard)

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID) =
      dayCardRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      dayCardRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      dayCardRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      dayCardRepository.deleteByVersion(identifier, version, projectIdentifier)
}
