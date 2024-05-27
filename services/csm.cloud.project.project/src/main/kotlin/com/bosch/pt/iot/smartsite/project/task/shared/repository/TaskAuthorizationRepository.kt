/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.repository

import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asUuidIds
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskAuthorizationDto
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Component
open class TaskAuthorizationRepository(
    private val taskAuthorizationCache: TaskAuthorizationCache,
    private val taskRepository: TaskRepository
) {

  open fun getTaskAuthorizations(taskIdentifiers: Set<TaskId>): Collection<TaskAuthorizationDto> =
      if (isWebRequestOrKafkaRequestScope)
          taskAuthorizationCache.getTaskAuthorizations(taskIdentifiers.asUuidIds())
      else taskRepository.findAllForAuthorizationByIdentifierIn(taskIdentifiers)

  open fun findTaskAuthorizationDto(taskIdentifier: TaskId): TaskAuthorizationDto? =
      getTaskAuthorizations(setOf(taskIdentifier)).firstOrNull()

  private val isWebRequestOrKafkaRequestScope: Boolean
    get() = RequestContextHolder.getRequestAttributes() != null
}
