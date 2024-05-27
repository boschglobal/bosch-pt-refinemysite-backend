/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isUnassignTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UnassignTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasUnassignPermissionOnTask(#identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey identifier: TaskId) {
    snapshotStore
        .findOrFail(identifier)
        .toCommandHandler()
        .checkPrecondition { isUnassignTaskPossible(it.status) }
        .onFailureThrow(
            TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED)
        .update {
          LOGGER.info("Task {} is now unassigned", it)
          it.copy(assigneeIdentifier = null)
        }
        .emitEvent(UNASSIGNED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UnassignTaskCommandHandler::class.java)
  }
}
