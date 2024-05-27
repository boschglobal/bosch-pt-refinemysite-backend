/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.helper

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OF_SAME_TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN_OR_DONE
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.daycard.authorization.DayCardAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardAuthorizationDto
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component
class DayCardCommandHandlerHelper(
    private val projectRepository: ProjectRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val dayCardRepository: DayCardRepository,
    private val dayCardAuthorizationComponent: DayCardAuthorizationComponent
) {

  fun authorizeCancellation(dayCards: Set<DayCardAuthorizationDto>) {
    val openDayCardIdentifiers =
        dayCards
            .filter { it.status == DayCardStatusEnum.OPEN }
            .map { requireNotNull(it.identifier) }
            .toSet()

    val doneDayCardIdentifiers =
        dayCards
            .filter { it.status == DayCardStatusEnum.DONE }
            .map { requireNotNull(it.identifier) }
            .toSet()

    if (dayCards.size != openDayCardIdentifiers.size + doneDayCardIdentifiers.size) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OPEN_OR_DONE)
    }

    if (openDayCardIdentifiers.isNotEmpty() &&
        !dayCardAuthorizationComponent.hasContributePermissionOnDayCards(openDayCardIdentifiers)) {
      throwAccessDeniedException()
    }
    if (doneDayCardIdentifiers.isNotEmpty() &&
        !dayCardAuthorizationComponent.hasReviewPermissionOnDayCards(doneDayCardIdentifiers)) {
      throwAccessDeniedException()
    }
  }

  fun assertDayCardsBelongToSameTask(dayCards: Set<DayCard>) {
    val tasksCount =
        dayCards.map { dayCard: DayCard -> dayCard.taskSchedule.task.identifier }.distinct().count()

    if (tasksCount > 1) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OF_SAME_TASK)
    }
  }

  fun getEtag(dayCard: DayCard, versionedIdentifiers: Collection<VersionedIdentifier>): ETag =
      ETag.from(
          (versionedIdentifiers.firstOrNull {
                requireNotNull(dayCard.identifier) == it.id.asDayCardId()
              }
                  ?: throw IllegalArgumentException("Identifier not found"))
              .version)

  fun findDayCardsOrFail(dayCardIdentifiers: Set<DayCardId>): Set<DayCard> =
      dayCardRepository.findAllEntitiesWithDetailsByIdentifierIn(dayCardIdentifiers).apply {
        if (dayCardIdentifiers.size != this.size) {
          throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_FOUND)
        }
      }

  fun findDayCardOrFail(dayCardIdentifier: DayCardId): DayCard =
      dayCardRepository.findEntityByIdentifier(dayCardIdentifier)
          ?: throw AggregateNotFoundException(
              DAY_CARD_VALIDATION_ERROR_NOT_FOUND, dayCardIdentifier.toString())

  fun checkProjectExistsOrFail(projectIdentifier: ProjectId) {
    val projectExists = projectRepository.existsByIdentifier(projectIdentifier)
    if (!projectExists) {
      throw PreconditionViolationException(COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND)
    }
  }

  fun findTaskScheduleOrFail(taskIdentifier: TaskId): TaskSchedule =
      taskScheduleRepository.findOneByTaskIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  private fun throwAccessDeniedException(): Unit =
      throw AccessDeniedException("User is not permitted to access the day card")
}
