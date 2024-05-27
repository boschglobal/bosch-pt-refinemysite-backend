/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository

import com.bosch.pt.iot.smartsite.common.repository.AbstractCache
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskAuthorizationDto
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
open class TaskAuthorizationCache(private val taskRepository: TaskRepository) :
    AbstractCache<UUID, TaskAuthorizationDto>() {

  open fun getTaskAuthorizations(taskIdentifiers: Set<UUID>): Collection<TaskAuthorizationDto> =
      get(taskIdentifiers)

  override fun loadFromDatabase(keys: Set<UUID>): Set<TaskAuthorizationDto> =
      taskRepository.findAllForAuthorizationByIdentifierIn(keys.asTaskIds())

  override fun getCacheKey(value: TaskAuthorizationDto): UUID = value.taskIdentifier.identifier
}
