/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.RESENT
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.ResendInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.facade.job.InvitationExpirationJob.Companion.EXPIRE_AFTER_DAYS
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import datadog.trace.api.Trace
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ResendInvitationCommandHandler(
    private val invitationSnapshotStore: InvitationSnapshotStore,
    private val participantMailService: ParticipantMailService,
    private val participantRepository: ParticipantRepository,
    private val eventBus: ProjectInvitationContextLocalEventBus,
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@participantAuthorizationComponent.hasUpdatePermissionOnParticipant(#command.identifier)")
  open fun handle(command: ResendInvitationCommand) {
    invitationSnapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .update { it.copy(lastSent = now()) }
        .emitEvent(RESENT)
        .to(eventBus)
        .withSideEffects { sendInvitationMail(it) }
  }

  private fun sendInvitationMail(invitation: InvitationSnapshot) {
    participantMailService.sendParticipantInvited(
        invitation.projectRef,
        invitation.email,
        invitation.lastSent.plusExpirationDuration(),
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            SecurityContextHelper.getInstance().getCurrentUser().identifier!!,
            invitation.projectRef)!!)
  }

  private fun LocalDateTime.plusExpirationDuration() = plusDays(EXPIRE_AFTER_DAYS)
}
