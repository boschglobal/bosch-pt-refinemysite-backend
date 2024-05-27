/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_INACTIVE_PARTICIPANT_NOT_CHANGEABLE
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_CHANGEABLE
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.UpdateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INACTIVE
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateParticipantCommandHandler(
    private val participantSnapshotStore: ParticipantSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@participantAuthorizationComponent.hasUpdatePermissionOnParticipant(#command.identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(
      @AuthorizationCacheKey("identifier") command: UpdateParticipantCommand
  ): ParticipantId =
      participantSnapshotStore
          .findOrFail(command.identifier)
          .toCommandHandler()
          .assertVersionMatches(command.version)
          .checkPrecondition { it.isNotInactive() }
          .onFailureThrow(PARTICIPANT_VALIDATION_ERROR_INACTIVE_PARTICIPANT_NOT_CHANGEABLE)
          .checkPrecondition { it.isNotCurrentUsersParticipant() }
          .onFailureThrow(PARTICIPANT_VALIDATION_ERROR_OWN_PARTICIPANT_NOT_CHANGEABLE)
          .update { it.copy(role = command.role) }
          .emitEvent(UPDATED)
          .ifSnapshotWasChanged()
          .to(eventBus)
          .andReturnSnapshot()
          .identifier

  private fun ParticipantSnapshot.isNotInactive() = status != INACTIVE

  private fun ParticipantSnapshot.isNotCurrentUsersParticipant() =
      userRef == null || userRef != getCurrentUser().identifier!!.asUserId()
}
