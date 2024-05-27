/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.authorization

import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.repository.findLatest
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.util.UUID
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class TaskActivityAuthorizationComponent(
    val participantRepository: ParticipantRepository,
    val taskRepository: TaskRepository
) {
  fun hasReadPermissionOnTask(taskIdentifier: UUID?): Boolean {
    if (taskIdentifier == null) {
      throw ResourceNotFoundException(TASK_VALIDATION_ERROR_NOT_FOUND)
    }

    val task = taskRepository.findLatest(taskIdentifier)

    val user = SecurityContextHolder.getContext().authentication.principal as User

    return task?.let { isParticipantOfProject(user.identifier, it.projectIdentifier) }
        ?: throw ResourceNotFoundException(TASK_VALIDATION_ERROR_NOT_FOUND)
  }

  private fun isParticipantOfProject(userIdentifier: UUID, projectIdentifier: UUID) =
      participantRepository.findOneCachedByProjectIdentifierAndUserIdentifierAndActiveTrue(
          projectIdentifier, userIdentifier) != null
}
