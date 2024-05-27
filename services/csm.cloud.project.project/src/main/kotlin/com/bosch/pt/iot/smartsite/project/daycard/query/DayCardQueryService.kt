/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.daycard.query

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class DayCardQueryService(private val dayCardRepository: DayCardRepository) {

  @Trace
  @PreAuthorize("@dayCardAuthorizationComponent.hasViewPermissionOnDayCard(#dayCardIdentifier)")
  @ExcludeFromCodeCoverage
  @Transactional(readOnly = true)
  open fun findByIdentifier(dayCardIdentifier: DayCardId): DayCard =
      dayCardRepository.findEntityByIdentifier(dayCardIdentifier)
          ?: throw AggregateNotFoundException(
              DAY_CARD_VALIDATION_ERROR_NOT_FOUND, dayCardIdentifier.toString())

  @Trace
  @PreAuthorize("@dayCardAuthorizationComponent.hasViewPermissionOnDayCard(#dayCardIdentifier)")
  @ExcludeFromCodeCoverage
  @Transactional(readOnly = true)
  open fun findWithDetails(dayCardIdentifier: DayCardId): DayCard =
      dayCardRepository.findEntityWithDetailsByIdentifier(dayCardIdentifier)
          ?: throw AggregateNotFoundException(
              DAY_CARD_VALIDATION_ERROR_NOT_FOUND, dayCardIdentifier.toString())

  @Trace
  @PreAuthorize("@dayCardAuthorizationComponent.hasViewPermissionOnDayCards(#dayCardIdentifiers)")
  @Transactional(readOnly = true)
  open fun findAllByIdentifierIn(dayCardIdentifiers: Set<DayCardId>): List<DayCardDto> =
      dayCardRepository.findAllByIdentifierIn(dayCardIdentifiers)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllWithDetailsByTaskIdentifier(taskIdentifier: TaskId): Set<DayCard> =
      dayCardRepository.findAllEntitiesWithDetailsByTaskIdentifier(taskIdentifier)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findAllWithDetailsByTaskIdentifiers(taskIdentifiers: Collection<TaskId>): Set<DayCard> =
      dayCardRepository.findAllEntitiesWithDetailsByTaskIdentifierIn(taskIdentifiers)

  @PreAuthorize("@dayCardAuthorizationComponent.hasViewPermissionOnDayCard(#dayCardIdentifier)")
  @Transactional(readOnly = true)
  open fun findTaskIdentifier(dayCardIdentifier: DayCardId): TaskId? =
      dayCardRepository.findDayCardTaskIdentifierByDayCardIdentifier(dayCardIdentifier)
}
