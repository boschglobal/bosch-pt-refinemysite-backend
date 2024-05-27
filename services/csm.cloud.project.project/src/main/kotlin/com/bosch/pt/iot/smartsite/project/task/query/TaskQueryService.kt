/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.query

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable_
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CR
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.CSM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import datadog.trace.api.Trace
import java.util.Comparator.comparingInt
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.asc
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskQueryService(
    private val taskRepository: TaskRepository,
    private val participantRepository: ParticipantRepository
) {

  /**
   * Finds all tasks that the current user is allowed to see and that match all of the filtering
   * criteria. All filter criteria can be null (lists can also be empty) in which case the filter
   * will be ignored. The filter date range is matched as follows: if start and end are specified,
   * it will show all tasks that somehow overlap with this range, i.e. either start or end in the
   * range or are "active" during the entire range. If only start is specified all tasks that start
   * or end is after the specified date are shown. Respectively for a sole end date.
   */
  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasViewPermissionOnTasksOfProject(#searchTasksDto.projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findTasksWithDetailsForFilters(
      searchTasksDto: SearchTasksDto,
      pageable: Pageable = Pageable.ofSize(Int.MAX_VALUE)
  ): Page<Task> {
    val taskFilters = searchTasksDto.toTaskFilterDto()
    val orderedTaskIds = taskRepository.findTaskIdsForFilters(taskFilters, pageable)
    val totalCount = taskRepository.countAllForFilters(taskFilters)
    val tasksWithDetails = taskRepository.findAllWithDetailsByIdIn(orderedTaskIds)

    return PageImpl(
        tasksWithDetails.sortedWith(comparingInt { orderedTaskIds.indexOf(it.id!!) }),
        pageable,
        totalCount)
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findTask(taskIdentifier: TaskId): Task =
      taskRepository.findOneByIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  fun findProjectIdentifierByIdentifier(taskIdentifier: TaskId): ProjectId =
      taskRepository.findProjectIdentifierByIdentifier(taskIdentifier)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findTasks(taskIdentifiers: Collection<TaskId>) =
      taskRepository.findAllByIdentifierIn(taskIdentifiers)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasksOfProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findTasksByProjectIdentifier(projectIdentifier: ProjectId) =
      taskRepository.findAllByProjectIdentifier(projectIdentifier)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasksOfProject(#projectIdentifier)")
  @Transactional(readOnly = true)
  open fun findTasks(
      projectIdentifier: ProjectId,
      showBasedOnRole: Boolean?,
      pageable: Pageable
  ): Page<Task> {

    // Sort by id to have a deterministic order
    val pageRequest: Pageable =
        PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            pageable.sort.and(Sort.by(asc(AbstractPersistable_.id.name))))
    val participant =
        participantRepository.findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
            getCurrentUser().identifier!!, projectIdentifier)!!

    if (showBasedOnRole != null && showBasedOnRole) {
      when (participant.role) {
        FM ->
            return taskRepository.findAllByProjectIdentifierAndAssigneeIdentifier(
                projectIdentifier, participant.identifier, pageRequest)
        CR ->
            return taskRepository.findAllByProjectIdentifierAndAssigneeCompanyIdentifier(
                projectIdentifier, participant.company!!.identifier!!, pageRequest)
        CSM -> {}
        else ->
            throw IllegalStateException("The project participant has an unexpected project role.")
      }
    }

    return taskRepository.findAllByProjectIdentifier(projectIdentifier, pageRequest)
  }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findTaskWithDetails(taskIdentifier: TaskId): Task =
      taskRepository.findOneWithDetailsByIdentifier(taskIdentifier)
          ?: throw AggregateNotFoundException(
              TASK_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findTasksWithDetails(taskIdentifiers: List<TaskId>) =
      taskRepository.findAllWithDetailsByIdentifierIn(taskIdentifiers)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#identifiers)")
  open fun findBatch(identifiers: Set<TaskId>, projectIdentifier: ProjectId): List<Task> =
      taskRepository.findAllWithDetailsByIdentifierInAndProjectIdentifier(
          identifiers, projectIdentifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun countByProjectIdentifier(projectIdentifier: ProjectId): Long =
      taskRepository.countByProjectIdentifier(projectIdentifier)
}
