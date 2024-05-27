/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.authorization

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskSpecifications.equalsId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskAuthorizationDto
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import datadog.trace.api.Trace
import java.util.UUID
import java.util.function.Predicate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class TaskAuthorizationComponent(
    private val taskRepository: TaskRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository,
    private val taskAuthorizationRepository: TaskAuthorizationRepository,
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {

  open fun hasEditPermissionOnTasks(taskIdentifiers: Set<TaskId>): Boolean =
      taskIdentifiers.isEmpty() ||
          filterTasksWithEditPermission(taskIdentifiers).containsAll(taskIdentifiers)

  open fun hasViewPermissionOnTasks(taskIdentifiers: Set<TaskId>): Boolean =
      taskIdentifiers.isEmpty() ||
          filterTasksWithViewPermission(taskIdentifiers).containsAll(taskIdentifiers)

  open fun hasAssignPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserAssignPermission(it) }

  open fun hasSendPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserSendPermission(it) }

  open fun hasUnassignPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserUnassignPermission(it) }

  open fun hasContributePermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserContributePermission(it) }

  open fun hasDeletePermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserDeletePermission(it) }

  @Trace
  open fun hasDeletePermissionOnTask(taskId: Long): Boolean {
    val task: Task = taskRepository.findOne(equalsId(taskId)).orElse(null) ?: return false
    return hasDeletePermissionOnTask(task.identifier)
  }

  open fun hasDeletePermissionOnTasks(taskIdentifiers: List<TaskId>): Boolean {
    if (taskIdentifiers.isEmpty()) return false

    // Iterate over each taskIdentifier in the list
    for (taskIdentifier in taskIdentifiers) {
      // Check if the current user has delete permission on the task
      val hasDeletePermission =
          hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserDeletePermission(it) }

      // If the user doesn't have delete permission on any of the tasks, return false
      if (!hasDeletePermission) {
        return false
      }
    }

    // If the loop finishes without returning false, it means the user has delete permission
    // on all the tasks
    return true
  }

  @Trace
  open fun hasDeletePermissionOnTaskAttachment(attachmentIdentifier: UUID): Boolean =
      taskAttachmentRepository.findTaskIdentifierFromTaskAttachment(attachmentIdentifier).let {
        it != null && hasDeletePermissionOnTask(it)
      }

  open fun hasEditPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserEditPermission(it) }

  /**
   * Validates the permission to update a given task and to assign it to a given participant.
   *
   * If the given assignee is `null`, only the update permission is validated.
   *
   * @param taskIdentifier the identifier of the task
   * @param assigneeIdentifier the identifier of the assignee (participant), can be `null`.
   */
  open fun hasEditAndAssignPermissionOnTask(
      taskIdentifier: TaskId,
      assigneeIdentifier: ParticipantId?
  ): Boolean =
      hasEditPermissionOnTask(taskIdentifier) &&
          (assigneeIdentifier == null || hasAssignPermissionOnTask(taskIdentifier))

  @Trace
  open fun hasStatusChangePermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserStatusChangePermission(it) }

  @Trace
  open fun hasAcceptStatusPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserAcceptTaskPermission(it) }

  open fun hasViewPermissionOnTask(taskIdentifier: TaskId?): Boolean =
      hasPermissionOnTask(taskIdentifier) { this.hasCurrentUserViewPermission(it) }

  open fun hasViewPermissionOnTasksOfProject(projectIdentifier: ProjectId): Boolean =
      isCurrentUserActiveParticipantOfProject(projectIdentifier)

  @Trace
  open fun hasViewPermissionOnTaskAttachment(attachmentIdentifier: UUID): Boolean =
      taskAttachmentRepository.findTaskIdentifierFromTaskAttachment(attachmentIdentifier).let {
        it != null && hasViewPermissionOnTask(it)
      }

  open fun filterTasksWithAssignPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserAssignPermission(it)
      }

  open fun filterTasksWithSendPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) { this.hasCurrentUserSendPermission(it) }

  open fun filterTasksWithAcceptPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserAcceptTaskPermission(it)
      }

  open fun filterTasksWithUnassignPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserUnassignPermission(it)
      }

  open fun filterTasksWithContributePermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserContributePermission(it)
      }

  open fun filterTasksWithDeletePermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserDeletePermission(it)
      }

  open fun filterTasksWithEditPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) { this.hasCurrentUserEditPermission(it) }

  open fun filterTasksWithStatusChangePermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) {
        this.hasCurrentUserStatusChangePermission(it)
      }

  open fun filterTasksWithViewPermission(taskIdentifiers: Set<TaskId>): Set<TaskId> =
      filterTaskIdentifiersWithPermission(taskIdentifiers) { this.hasCurrentUserViewPermission(it) }

  open fun hasCurrentUserSendPermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return when (participant.role) {
      CSM -> true
      CR -> task.isAssignedToCompanyOf(participant)
      FM -> task.isAssignedTo(participant)
    }
  }

  open fun hasReschedulePermissionOnProject(projectIdentifier: ProjectId): Boolean {
    val participant: ParticipantAuthorizationDto =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
            ?: return false

    return participant.role == CSM
  }

  open fun hasBatchStatusUpdatePermissionOnProject(projectIdentifier: ProjectId): Boolean {
    val participant: ParticipantAuthorizationDto =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier)
            ?: return false

    return participant.role == CSM
  }

  private fun hasCurrentUserAssignPermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return when (participant.role) {
      CSM -> true
      CR ->
          task.isAssignedToCompanyOf(participant) ||
              task.isUnassigned() && task.isCreatedByCompanyOf(participant)
      FM -> task.isAssignedTo(participant) || task.isUnassigned() && task.isCreatedBy(participant)
    }
  }

  private fun hasCurrentUserUnassignPermission(task: TaskAuthorizationDto) =
      findParticipantOfCurrentUser(task)?.let { it.role == CSM } ?: false

  private fun hasCurrentUserContributePermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return when (participant.role) {
      CSM -> true
      CR -> task.isCreatedByCompanyOf(participant) || task.isAssignedToCompanyOf(participant)
      FM -> task.isCreatedBy(participant) || task.isAssignedTo(participant)
    }
  }

  private fun hasCurrentUserDeletePermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return when (participant.role) {
      CSM -> true
      CR ->
          task.isCreatedByCompanyOf(participant) &&
              (task.isUnassigned() || task.isAssignedToCompanyOf(participant))
      FM -> task.isCreatedBy(participant) && (task.isUnassigned() || task.isAssignedTo(participant))
    }
  }

  private fun hasCurrentUserEditPermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return if (task.isClosed() || task.isAccepted()) {
      false
    } else
        when (participant.role) {
          CSM -> true
          CR ->
              task.isCreatedByCompanyOf(participant) &&
                  (task.isUnassigned() || task.isAssignedToCompanyOf(participant))
          FM ->
              task.isCreatedBy(participant) &&
                  (task.isUnassigned() || task.isAssignedTo(participant))
        }
  }

  private fun hasCurrentUserStatusChangePermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return when (participant.role) {
      CSM -> true
      CR ->
          task.isAssignedToCompanyOf(participant) &&
              task.taskStatus !== DRAFT &&
              task.taskStatus !== ACCEPTED
      FM ->
          task.isAssignedTo(participant) &&
              task.taskStatus !== DRAFT &&
              task.taskStatus !== ACCEPTED
    }
  }

  private fun hasCurrentUserAcceptTaskPermission(task: TaskAuthorizationDto): Boolean {
    val participant: ParticipantAuthorizationDto =
        findParticipantOfCurrentUser(task) ?: return false

    return participant.role == CSM
  }

  private fun hasCurrentUserViewPermission(taskAuthorizationDto: TaskAuthorizationDto): Boolean =
      isCurrentUserActiveParticipantOfProject(taskAuthorizationDto.projectIdentifier)

  private fun isCurrentUserActiveParticipantOfProject(projectIdentifier: ProjectId): Boolean =
      participantAuthorizationRepository.getParticipantOfCurrentUser(projectIdentifier) != null

  private fun hasPermissionOnTask(
      taskIdentifier: TaskId?,
      permissionRule: Predicate<TaskAuthorizationDto>
  ): Boolean {
    if (taskIdentifier == null) return false
    val task = taskAuthorizationRepository.findTaskAuthorizationDto(taskIdentifier)
    return task != null && permissionRule.test(task)
  }

  private fun filterTaskIdentifiersWithPermission(
      taskIdentifiers: Set<TaskId>,
      permissionRule: Predicate<TaskAuthorizationDto>
  ): Set<TaskId> =
      if (taskIdentifiers.isEmpty()) emptySet()
      else
          taskAuthorizationRepository
              .getTaskAuthorizations(taskIdentifiers)
              .filter { permissionRule.test(it) }
              .map { it.taskIdentifier }
              .toSet()

  private fun findParticipantOfCurrentUser(
      task: TaskAuthorizationDto
  ): ParticipantAuthorizationDto? =
      participantAuthorizationRepository.getParticipantOfCurrentUser(task.projectIdentifier)
}
