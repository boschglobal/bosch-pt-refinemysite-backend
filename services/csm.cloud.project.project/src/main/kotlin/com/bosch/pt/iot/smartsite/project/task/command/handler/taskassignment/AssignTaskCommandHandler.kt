/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_ASSIGNMENT_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isAssignTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AssignTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val participantRepository: ParticipantRepository
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasAssignPermissionOnTask(#identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey identifier: TaskId, assigneeIdentifier: ParticipantId) {
    snapshotStore
        .findOrFail(identifier)
        .toCommandHandler()
        .checkPrecondition { isAssignTaskPossible(it.status) }
        .onFailureThrow(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_ASSIGNMENT_FORBIDDEN)
        .checkPrecondition {
          participantRepository.existsByIdentifierAndProjectIdAndParticipantStatusIsActive(
              assigneeIdentifier, it.projectIdentifier)
        }
        .onFailureThrow(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)
        .update {
          LOGGER.info("Assigning {} to task {}", it.assigneeIdentifier, it)
          it.copy(assigneeIdentifier = assigneeIdentifier)
        }
        .emitEvent(ASSIGNED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AssignTaskCommandHandler::class.java)
  }
}
