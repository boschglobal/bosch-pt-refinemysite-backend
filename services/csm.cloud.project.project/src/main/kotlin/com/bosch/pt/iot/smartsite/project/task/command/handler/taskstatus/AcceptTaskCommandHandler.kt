/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isAcceptTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AcceptTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasAcceptStatusPermissionOnTask(#identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey identifier: TaskId) {
    snapshotStore
        .findOrFail(identifier)
        .toCommandHandler()
        .checkPrecondition { isAcceptTaskPossible(it.status) }
        .onFailureThrow(TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN)
        .update {
          LOGGER.info(TASK_STATUS_CHANGE_MSG, it, it.status, ACCEPTED)
          it.copy(status = ACCEPTED)
        }
        .emitEvent(TaskEventEnumAvro.ACCEPTED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  companion object {
    private val LOGGER = getLogger(AcceptTaskCommandHandler::class.java)

    private const val TASK_STATUS_CHANGE_MSG = "Changed task {} from status {} to status {}"
  }
}
