/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isStartTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class StartTaskCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasStatusChangePermissionOnTask(#identifier)")
  @InvalidatesAuthorizationCache
  open fun handle(@AuthorizationCacheKey identifier: TaskId) {
    snapshotStore
        .findOrFail(identifier)
        .toCommandHandler()
        .checkPrecondition { isStartTaskPossible(it.status) }
        .onFailureThrow(TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN)
        .update {
          LOGGER.info(TASK_STATUS_CHANGE_MSG, it, it.status, STARTED)
          it.copy(status = TaskStatusEnum.STARTED)
        }
        .emitEvent(STARTED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  companion object {
    private val LOGGER = getLogger(StartTaskCommandHandler::class.java)

    private const val TASK_STATUS_CHANGE_MSG = "Changed task {} from status {} to status {}"
  }
}
