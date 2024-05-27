/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_OPEN_POSSIBLE_WHEN_STATUS_DRAFT
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isSendTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class SendTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val participantRepository: ParticipantRepository
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasSendPermissionOnTask(#identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey identifier: TaskId) {
    val task = snapshotStore.findOrFail(identifier)

    if (task.assigneeIdentifier != null) {
      existsActiveParticipantOrFail(task.assigneeIdentifier)
    }

    snapshotStore
        .findOrFail(identifier)
        .toCommandHandler()
        .checkPrecondition { isSendTaskPossible(it.status) }
        .onFailureThrow(TASK_VALIDATION_ERROR_OPEN_POSSIBLE_WHEN_STATUS_DRAFT)
        .update {
          LOGGER.info(TASK_STATUS_CHANGE_MSG, it, DRAFT, OPEN)
          it.copy(status = OPEN)
        }
        .emitEvent(SENT)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun existsActiveParticipantOrFail(participantIdentifier: ParticipantId) {
    if (!participantRepository.existsByIdentifierAndParticipantStatusIsActive(
        participantIdentifier)) {
      throw PreconditionViolationException(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)
    }
  }

  companion object {
    private val LOGGER = getLogger(SendTaskCommandHandler::class.java)

    private const val TASK_STATUS_CHANGE_MSG = "Changed task {} from status {} to status {}"
  }
}
