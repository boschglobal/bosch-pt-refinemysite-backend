/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_ACCEPT_INVITATION_ONLY_IN_STATUS_INVITED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.AcceptInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Component
open class AcceptInvitationCommandHandler(
    private val invitationSnapshotStore: InvitationSnapshotStore,
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val userRepository: UserRepository,
    private val eventBus: ProjectContextLocalEventBus,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun handle(command: AcceptInvitationCommand) {
    val invitation = invitationSnapshotStore.findOrFail(command.identifier)

    participantSnapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .checkPrecondition { it.status == INVITED }
        .onFailureThrow(PARTICIPANT_VALIDATION_ERROR_ACCEPT_INVITATION_ONLY_IN_STATUS_INVITED)
        .update {
          it.copy(
              userRef = userRepository.findOneByEmail(invitation.email)!!.identifier!!.asUserId(),
              status = VALIDATION)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .withSideEffects { logStatusChange(it) }
  }

  private fun logStatusChange(participant: ParticipantSnapshot) =
      logger.info(
          "Participant for project % and user % set to \"IN VALIDATION\"",
          participant.projectRef,
          participant.userRef)
}
