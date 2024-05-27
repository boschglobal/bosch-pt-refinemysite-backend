/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.daycard.authorization

import com.bosch.pt.iot.smartsite.common.authorization.AuthorizationDelegation.delegateAuthorizationForIdentifiers
import com.bosch.pt.iot.smartsite.common.uuid.toUuids
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asUuidIds
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class DayCardAuthorizationComponent(
    private val dayCardRepository: DayCardRepository,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  open fun hasViewPermissionOnDayCard(dayCardIdentifier: DayCardId): Boolean =
      dayCardRepository.findDayCardTaskIdentifierByDayCardIdentifier(dayCardIdentifier).let {
        it != null && taskAuthorizationComponent.hasViewPermissionOnTask(it)
      }

  open fun hasViewPermissionOnDayCards(dayCardIdentifiers: Set<DayCardId>): Boolean =
      delegateAuthorizationForIdentifiers(
              dayCardIdentifiers.asUuidIds(),
              {
                dayCardRepository.findDayCardTaskIdentifiersByDayCardIdentifiers(
                    it.map { it.asDayCardId() })
              }) {
                taskAuthorizationComponent.filterTasksWithViewPermission(it.asTaskIds()).toUuids()
              }
          .containsAll(dayCardIdentifiers.asUuidIds())

  open fun hasContributePermissionOnDayCard(dayCardIdentifier: DayCardId): Boolean =
      dayCardRepository.findDayCardTaskIdentifierByDayCardIdentifier(dayCardIdentifier).let {
        it != null && taskAuthorizationComponent.hasContributePermissionOnTask(it)
      }

  open fun hasContributePermissionOnDayCards(dayCardIdentifiers: Set<DayCardId>): Boolean =
      delegateAuthorizationForIdentifiers(
              dayCardIdentifiers.toUuids(),
              {
                dayCardRepository.findDayCardTaskIdentifiersByDayCardIdentifiers(
                    it.map { it.asDayCardId() })
              }) {
                taskAuthorizationComponent
                    .filterTasksWithContributePermission(it.asTaskIds())
                    .toUuids()
              }
          .containsAll(dayCardIdentifiers.map { it.identifier })

  /**
   * Returns whether the current user is permitted to apply status changes related to reviewing a
   * day card:
   * * Cancel from done: DONE -> NOT DONE
   * * Approve: {OPEN, DONE} -> APPROVED
   * * Reset: {NOT DONE, DONE, APPROVED} -> OPEN
   *
   * @return whether the current user is permitted to apply the status change on the specified day
   *   card.
   */
  open fun hasReviewPermissionOnDayCard(dayCardIdentifier: DayCardId): Boolean =
      dayCardRepository.findDayCardProjectIdentifierByDayCardIdentifier(dayCardIdentifier).let {
        it != null && hasReviewPermissionOnDayCardsOfProject(it)
      }

  open fun hasReviewPermissionOnDayCards(dayCardIdentifiers: Set<DayCardId>): Boolean =
      hasReviewPermissionOnDayCardsOfProjects(
          dayCardRepository.findDayCardProjectIdentifiersByDayCardIdentifiers(dayCardIdentifiers))

  open fun hasReviewPermissionOnDayCardsOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier).let {
        it != null && it.role == CSM
      }

  private fun hasReviewPermissionOnDayCardsOfProjects(projectIdentifiers: Set<ProjectId>): Boolean =
      projectIdentifiers
          .map { participantAuthorizationRepository.getParticipantOfCurrentUser(it) }
          .all { it != null && it.role == CSM }
}
