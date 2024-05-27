/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_ACTIVATE_ONLY_IN_STATUS_VALIDATION
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.ActivateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
open class ActivateParticipantCommandHandler(
    private val participantRepository: ParticipantRepository,
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val participantMailService: ParticipantMailService,
    private val invitationRepository: InvitationRepository,
    private val invitationSnapshotStore: InvitationSnapshotStore,
    private val projectEventBus: ProjectContextLocalEventBus,
    private val invitationEventBus: ProjectInvitationContextLocalEventBus,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = Propagation.MANDATORY)
  open fun handle(command: ActivateParticipantCommand) {
    participantSnapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .checkPrecondition { it.status == VALIDATION }
        .onFailureThrow(PARTICIPANT_VALIDATION_ERROR_ACTIVATE_ONLY_IN_STATUS_VALIDATION)
        .update { it.copy(companyRef = command.companyRef, status = ACTIVE) }
        .emitEvent(UPDATED)
        .to(projectEventBus)
        .withSideEffects {
          sendActivationMail(it.identifier)
          logParticipantActivation(it)
        }

    // notice 1: deletion comes last because after deletion we are no longer able to determine the
    // last inviting participant
    // notice 2: invitation could be null if there is a duplicate invitation event in the stream,
    // or if the same invitation event is processed twice
    invitationSnapshotStore
        .findOrIgnore(command.identifier)
        ?.toCommandHandler()
        ?.emitTombstone()
        ?.to(invitationEventBus)
  }

  private fun sendActivationMail(participantId: ParticipantId) {
    participantRepository.findOneWithDetailsByIdentifier(participantId)!!.let {
      participantMailService.sendParticipantActivated(
          it.project!!.identifier, it, findLastInvitingParticipant(it))
    }
  }

  private fun logParticipantActivation(participantSnapshot: ParticipantSnapshot) {
    logger.info(
        "Participant for project %, company % and user % set to \"ACTIVE\"",
        participantSnapshot.projectRef,
        participantSnapshot.companyRef,
        participantSnapshot.userRef)
  }

  /**
   * If an invitation was resent, the participant who triggered the latest resend is returned;
   * otherwise, return the original inviting participant.
   */
  private fun findLastInvitingParticipant(invitedParticipant: Participant): Participant? {
    val invitation =
        invitationRepository.findOneByParticipantIdentifier(invitedParticipant.identifier)

    /* Note: the invitation can be null in this edge case:
     * 1. the participant was invited after the user signed up already
     * 2. and the user was not yet assigned to a company
     */
    val lastInvitingUser =
        if (invitation != null) {
          invitation.lastModifiedBy.orElseThrow()
        } else {
          invitedParticipant.createdBy.orElseThrow()
        }
    val project = invitedParticipant.project!!
    return participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
        lastInvitingUser.identifier, project.identifier)
  }
}
