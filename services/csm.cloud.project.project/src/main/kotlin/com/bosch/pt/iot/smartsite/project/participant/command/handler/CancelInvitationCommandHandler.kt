/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_CANCEL_ONLY_IN_STATUS_INVITED_OR_VALIDATION
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.command.api.CancelInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.VALIDATION
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CancelInvitationCommandHandler(
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val invitationSnapshotStore: InvitationSnapshotStore,
    private val projectEventBus: ProjectContextLocalEventBus,
    private val projectInvitationEventBus: ProjectInvitationContextLocalEventBus,
    private val logger: Logger
) {

  @Trace
  @NoPreAuthorize
  @Transactional
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey("identifier") command: CancelInvitationCommand) {

    participantSnapshotStore
        .findOrIgnore(command.identifier)
        ?.toCommandHandler()
        ?.checkPrecondition { it.isNotYetActive() }
        ?.onFailureThrow(PARTICIPANT_VALIDATION_ERROR_CANCEL_ONLY_IN_STATUS_INVITED_OR_VALIDATION)
        ?.emitEvent(CANCELLED)
        ?.to(projectEventBus)
        ?: logParticipantNotFoundWarning()

    invitationSnapshotStore
        .findOrIgnore(command.identifier)
        ?.toCommandHandler()
        ?.emitTombstone()
        ?.to(projectInvitationEventBus)
        ?: logInvitationNotFoundWarning()
  }

  private fun ParticipantSnapshot.isNotYetActive() = status in setOf(INVITED, VALIDATION)

  private fun logParticipantNotFoundWarning() {
    logger.warn("Tried to cancel invitation for participant but no participant was found.")
  }

  private fun logInvitationNotFoundWarning() {
    logger.warn("Tried to cancel invitation for participant but no invitation was found.")
  }
}
