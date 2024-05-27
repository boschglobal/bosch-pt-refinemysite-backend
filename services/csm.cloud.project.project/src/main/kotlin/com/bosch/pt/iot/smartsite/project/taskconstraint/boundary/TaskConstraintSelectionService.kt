/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskconstraint.boundary

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.dto.TaskConstraintSelectionRootProjection
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintSelectionRepository
import datadog.trace.api.Trace
import java.util.UUID.randomUUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
open class TaskConstraintSelectionService(
    private val idGenerator: IdGenerator,
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    private val taskConstraintService: TaskConstraintService,
    private val taskRepository: TaskRepository
) {

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findSelection(taskIdentifier: TaskId): TaskConstraintSelectionDto? =
      taskConstraintSelectionRepository.findOneWithDetailsByTaskIdentifier(taskIdentifier)?.let {
        TaskConstraintSelectionDto(
            it.identifier!!, it.version!!, taskIdentifier, sort(it.constraints.toList()))
      }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findSelections(taskIdentifiers: Set<TaskId>): List<TaskConstraintSelectionDto> {
    if (taskIdentifiers.isEmpty()) {
      return emptyList()
    }

    val constraintSelectionRoots =
        taskConstraintSelectionRepository
            .findTaskConstraintSelectionRootProjectionByTaskIdentifierIn(taskIdentifiers)

    // Check if selections were found for all requested task ids
    val foundSelectionTaskIdentifiers = constraintSelectionRoots.map { it.taskIdentifier }

    // Create fake selections for tasks without persisted selections
    val fakeSelections =
        taskIdentifiers
            .filter { !foundSelectionTaskIdentifiers.contains(it) }
            .map { TaskConstraintSelectionRootProjection(randomUUID(), 0L, it) }

    // Merge found selections with fake selections
    val constraintSelections = constraintSelectionRoots.toMutableList()
    constraintSelections.addAll(fakeSelections)

    // Load the constraints of the selections
    val constraintSelectionConstraints =
        taskConstraintSelectionRepository
            .findConstraintSelectionConstraintProjectionByTaskIdentifierIn(taskIdentifiers)
            .groupByTo(mutableMapOf(), { it.selectionIdentifier }, { it.constraint })

    return constraintSelections
        .map {
          TaskConstraintSelectionDto(
              it.identifier,
              it.version,
              it.taskIdentifier,
              constraintSelectionConstraints[it.identifier]?.let { sort(it) } ?: emptyList())
        }
        .sortedBy { it.identifier }
  }

  /**
   * Creates a constraint selection with an empty set of constraints if no constraint selection was
   * found and the given set of constraints is not empty for the given task identifier.
   *
   * By default - we return an empty resource in the find methods with version 0 even if there isn't
   * an entity in the database.
   *
   * Therefore, this method should always be called before the updateSelection method to ensure that
   * there's a collection when the update is executed and to ensure that the result of the update
   * operation has a new entity version.
   *
   * The constraints have to be updated separately by calling the updateSelection method.
   */
  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasContributePermissionOnTask(#taskIdentifier)")
  open fun createEmptySelectionIfNotExists(
      taskIdentifier: TaskId,
      constraints: Set<TaskConstraintEnum>
  ) {
    val constraintSelection =
        taskConstraintSelectionRepository.findOneWithDetailsByTaskIdentifier(taskIdentifier)

    if (constraintSelection == null && constraints.isNotEmpty()) {
      createSelection(taskIdentifier, emptySet())
    }
  }

  @Trace
  @Transactional
  @PreAuthorize("@taskAuthorizationComponent.hasContributePermissionOnTask(#taskIdentifier)")
  open fun updateSelection(
      projectIdentifier: ProjectId,
      taskIdentifier: TaskId,
      constraints: Set<TaskConstraintEnum>,
      eTag: ETag
  ) {
    val constraintSelection =
        taskConstraintSelectionRepository.findOneWithDetailsByTaskIdentifier(taskIdentifier)

    constraintSelection?.let { eTag.verify(it) }

    if (constraints.isEmpty()) {
      if (constraintSelection != null) {
        taskConstraintSelectionRepository.delete(constraintSelection, DELETED)
      } else {
        // Do nothing
      }
      return
    }

    assertOnlyActiveConstraintsSelected(projectIdentifier, constraints)

    if (constraintSelection != null) {
      updateSelection(constraintSelection, constraints)
    } else {
      throw IllegalStateException(
          "A constraint selection must be created before calling updateSelection")
    }
  }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deletePartitioned(taskIds: List<Long>) {
    val ids: List<Long> = taskConstraintSelectionRepository.getIdsByTaskIdsPartitioned(taskIds)
    taskConstraintSelectionRepository.deleteConstraintElementsPartitioned(ids)
    taskConstraintSelectionRepository.deletePartitioned(ids)
  }

  private fun createSelection(taskIdentifier: TaskId, constraints: Set<TaskConstraintEnum>) {
    taskRepository.findOneByIdentifier(taskIdentifier)!!.also { task ->
      TaskConstraintSelection(task, constraints.toMutableSet()).also { selection ->
        selection.identifier = idGenerator.generateId()
        taskConstraintSelectionRepository.save(selection, CREATED)
      }
    }
  }

  private fun updateSelection(
      constraintSelection: TaskConstraintSelection,
      constraints: Set<TaskConstraintEnum>
  ) {
    val elementsToRemove = constraintSelection.constraints - constraints
    val elementsToAdd = constraints - constraintSelection.constraints

    if (elementsToRemove.isNotEmpty()) {
      constraintSelection.constraints.removeAll(elementsToRemove)
    }
    if (elementsToAdd.isNotEmpty()) {
      constraintSelection.constraints.addAll(elementsToAdd)
    }

    if (elementsToAdd.isNotEmpty() || elementsToRemove.isNotEmpty()) {
      // Don't send an event if nothing has changed
      taskConstraintSelectionRepository.save(constraintSelection, UPDATED)
    }
  }

  private fun assertOnlyActiveConstraintsSelected(
      projectIdentifier: ProjectId,
      constraintSelection: Set<TaskConstraintEnum>
  ) {
    val projectConstraints = taskConstraintService.findAll(projectIdentifier)
    val deactivatedProjectConstraints = projectConstraints.filter { !it.active }.map { it.key }
    val selectedInactiveConstraints =
        constraintSelection.filter { deactivatedProjectConstraints.contains(it) }

    if (selectedInactiveConstraints.isNotEmpty()) {
      throw PreconditionViolationException(Key.TASK_CONSTRAINT_VALIDATION_ERROR_REASON_DEACTIVATED)
    }
  }

  private fun sort(constraints: List<TaskConstraintEnum>) = constraints.sortedBy { it.ordinal }
}
