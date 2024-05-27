/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.command.service

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.authorization.cache.AuthorizationCacheKey
import com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache
import com.bosch.pt.iot.smartsite.common.boundary.AsynchronousDeleteServiceV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_TASK_WITH_NON_OPEN_DAY_CARDS_CANNOT_BE_DELETED
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.project.daycard.command.service.DayCardService
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isDeletePossible
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import datadog.trace.api.Trace
import org.slf4j.Logger
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskRequestDeleteService(
    private val taskRepository: TaskRepository,
    private val dayCardService: DayCardService,
    private val commandSendingService: CommandSendingService,
    private val logger: Logger
) : AsynchronousDeleteServiceV2<TaskId> {

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasDeletePermissionOnTask(#identifier)")
  @Transactional
  @InvalidatesAuthorizationCache
  override fun markAsDeletedAndSendEvent(@AuthorizationCacheKey identifier: TaskId) {
    val task =
        taskRepository.findOneByIdentifier(identifier)
            ?: throw AggregateNotFoundException(
                TASK_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

    // Check preconditions
    val aggregationMap = dayCardService.getStatusCountAggregation(listOf(identifier))
    if (!isDeletePossible(aggregationMap[identifier])) {
      throw PreconditionViolationException(
          TASK_VALIDATION_ERROR_TASK_WITH_NON_OPEN_DAY_CARDS_CANNOT_BE_DELETED)
    }

    // Create message to send
    val key = CommandMessageKey(task.project.identifier.toUuid())
    val deleteCommandAvro =
        DeleteCommandAvro(
            getAggregateIdentifier(task, TASK.value),
            getAggregateIdentifier(
                SecurityContextHelper.getInstance().getCurrentUser(), USER.value))

    // Mark the task as deleted without sending an event and increasing the hibernate version
    markAsDeleted(task.identifier)

    // Send message to delete the task
    commandSendingService.send(key, deleteCommandAvro, "project-delete")
    logger.info("Task {} was marked to be deleted", identifier)
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  override fun markAsDeleted(identifier: TaskId) {
    taskRepository.findOneByIdentifier(identifier)?.apply {
      taskRepository.markAsDeleted(this.id!!)
    }
  }
}
