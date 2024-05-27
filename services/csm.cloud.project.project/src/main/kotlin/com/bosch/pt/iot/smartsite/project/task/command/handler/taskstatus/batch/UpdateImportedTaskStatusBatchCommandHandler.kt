/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskStatusCommand
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.toCommandHandler
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import datadog.trace.api.Trace
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateImportedTaskStatusBatchCommandHandler(
    private val snapshotStore: TaskSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val taskRepository: TaskRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  /**
   * Updates the status of a batch of DRAFT [Task]. Used for data import.
   *
   * @throws [PreconditionViolationException] if a targeted task is not in DRAFT status
   */
  @Trace
  @Transactional
  @PreAuthorize(
      "@taskAuthorizationComponent.hasBatchStatusUpdatePermissionOnProject(#projectIdentifier)")
  open fun handle(
      projectIdentifier: ProjectId,
      commands: List<UpdateTaskStatusCommand>,
  ) {
    if (commands.isEmpty()) return

    businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
      commands.map { handle(it) }
    }
  }

  private fun handle(command: UpdateTaskStatusCommand): TaskId {
    val task = findTaskOrFail(command.identifier)
    require(task.status == DRAFT) {
      "Please consider that this batch status update was originally designed for imported DRAFT tasks."
    }

    LOGGER.info(TASK_STATUS_CHANGE_MSG, task, task.status, command.status)

    val event =
        when (command.status) {
          OPEN -> TaskEventEnumAvro.SENT
          STARTED -> TaskEventEnumAvro.STARTED
          CLOSED -> TaskEventEnumAvro.CLOSED
          ACCEPTED -> TaskEventEnumAvro.ACCEPTED
          else -> {
            return command.identifier
          }
        }

    return snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(task.version)
        .update { it.copy(status = command.status) }
        .emitEvent(event)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun findTaskOrFail(taskIdentifier: TaskId): Task =
      taskRepository.findOneByIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  companion object {
    private val LOGGER = getLogger(UpdateImportedTaskStatusBatchCommandHandler::class.java)

    private const val TASK_STATUS_CHANGE_MSG = "Changed task {} from status {} to status {}"
  }
}
