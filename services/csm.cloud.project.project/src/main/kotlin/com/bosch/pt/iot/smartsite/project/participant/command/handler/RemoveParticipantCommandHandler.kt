/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_DEACTIVATE_ONLY_IN_STATUS_ACTIVE
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_REMOVABLE
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_CSM_NOT_REMOVABLE
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.CancelInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.DeactivateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.api.RemoveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Component
open class RemoveParticipantCommandHandler(
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val projectEventBus: ProjectContextLocalEventBus,
    private val participantRepository: ParticipantRepository,
    private val cancelInvitationCommandHandler: CancelInvitationCommandHandler
) {

  @Trace
  @PreAuthorize(
      "@participantAuthorizationComponent" +
          ".hasDeletePermissionOnParticipant(#command.identifier)")
  @Transactional
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey("identifier") command: RemoveParticipantCommand) {
    val participant = participantRepository.findOneWithDetailsByIdentifier(command.identifier)
    requireNotNull(participant) { "Project participant must not be null" }
    if (participant.isNotYetActive()) {
      cancelInvitationCommandHandler.handle(CancelInvitationCommand(command.identifier))
    } else {
      assertAtLeastOneCsmRemains(participant)
      assertMustNotRemoveOwnParticipant(participant)
      deactivate(DeactivateParticipantCommand(command.identifier))
    }
  }

  private fun Participant.isNotYetActive() = status in setOf(INVITED, VALIDATION)

  @Trace
  @DenyWebRequests
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun handleFromMessaging(command: RemoveParticipantCommand) {
    participantRepository.findOneWithDetailsByIdentifier(command.identifier)?.let {
      deactivate(DeactivateParticipantCommand(it.identifier))
    }
  }

  private fun deactivate(command: DeactivateParticipantCommand) {
    participantSnapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .checkPrecondition { it.isActive() }
        .onFailureThrow(PARTICIPANT_VALIDATION_ERROR_DEACTIVATE_ONLY_IN_STATUS_ACTIVE)
        .update { it.copy(status = INACTIVE) }
        .emitEvent(DEACTIVATED)
        .to(projectEventBus)
  }

  private fun ParticipantSnapshot.isActive() = status == ACTIVE

  private fun assertAtLeastOneCsmRemains(participant: Participant) {
    if (participant.role == ParticipantRoleEnum.CSM &&
        participantRepository
            .findAllByProjectIdentifierInAndRoleAndActiveTrue(
                setOf(participant.project!!.identifier), ParticipantRoleEnum.CSM)
            .size < 2) {
      throw PreconditionViolationException(
          PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_CSM_NOT_REMOVABLE)
    }
  }

  private fun assertMustNotRemoveOwnParticipant(participant: Participant) {
    if (participant.user!!.identifier == getCurrentUser().identifier) {
      throw PreconditionViolationException(
          PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_REMOVABLE)
    }
  }
}
