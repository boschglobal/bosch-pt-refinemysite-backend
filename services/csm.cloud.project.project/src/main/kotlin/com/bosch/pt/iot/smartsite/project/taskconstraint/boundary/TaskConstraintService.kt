/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.TaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.dto.UpdateTaskConstraintDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
open class TaskConstraintService(
    private val constraintCustomizationRepository: TaskConstraintCustomizationRepository,
    private val projectRepository: ProjectRepository,
    private val idGenerator: IdGenerator,
    private val messageSource: MessageSource
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskConstraintAuthorizationComponent.hasUpdateConstraintPermissionOnProject(#constraint.projectIdentifier)")
  open fun update(constraint: UpdateTaskConstraintDto): TaskConstraintDto {
    assertAtLeastOneActiveConstraintRemains(constraint.projectIdentifier, constraint)

    val constraintCustomization =
        constraintCustomizationRepository.findOneByKeyAndProjectIdentifier(
            constraint.key, constraint.projectIdentifier)

    return if (constraint.key.isCustom) updateCustomConstraint(constraintCustomization, constraint)
    else updateStandardConstraint(constraintCustomization, constraint)
  }

  private fun updateStandardConstraint(
      constraintCustomization: TaskConstraintCustomization?,
      constraint: UpdateTaskConstraintDto
  ): TaskConstraintDto =
      if (constraintCustomization == null) {
        if (constraint.active) {
          TaskConstraintDto(constraint.key, true, null)
        } else {
          val project = projectRepository.findOneByIdentifier(constraint.projectIdentifier)!!
          TaskConstraintCustomization(project, constraint.key, constraint.active, null)
              .apply { identifier = idGenerator.generateId() }
              .let {
                constraintCustomizationRepository.save(it, CREATED)
                TaskConstraintDto(it.key, it.active, it.name)
              }
        }
      } else {
        if (constraint.active) {
          constraintCustomizationRepository.delete(constraintCustomization, DELETED)
          TaskConstraintDto(constraint.key, true, null)
        } else {
          TaskConstraintDto(constraint.key, constraintCustomization.active, null)
        }
      }

  private fun updateCustomConstraint(
      constraintCustomization: TaskConstraintCustomization?,
      constraint: UpdateTaskConstraintDto
  ): TaskConstraintDto =
      if (constraintCustomization == null) {
        if (constraint.active || !constraint.name.isNullOrEmpty()) {
          val project = projectRepository.findOneByIdentifier(constraint.projectIdentifier)!!
          TaskConstraintCustomization(project, constraint.key, constraint.active, constraint.name)
              .apply { identifier = idGenerator.generateId() }
              .let {
                constraintCustomizationRepository.save(it, CREATED)
                TaskConstraintDto(it.key, it.active, it.name)
              }
        } else {
          TaskConstraintDto(constraint.key, false, null)
        }
      } else {
        if (constraint.active || !constraint.name.isNullOrEmpty()) {
          constraintCustomization.active = constraint.active
          constraintCustomization.name = constraint.name
          constraintCustomizationRepository.save(constraintCustomization, UPDATED)
          TaskConstraintDto(
              constraintCustomization.key,
              constraintCustomization.active,
              constraintCustomization.name)
        } else {
          constraintCustomizationRepository.delete(constraintCustomization, DELETED)
          TaskConstraintDto(constraint.key, false, null)
        }
      }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@taskConstraintAuthorizationComponent.hasViewPermissionOnConstraint(#projectIdentifier)")
  open fun findAll(projectIdentifier: ProjectId): List<TaskConstraintDto> {
    val constraintCustomizations =
        constraintCustomizationRepository
            .findAllByProjectIdentifier(projectIdentifier)
            .associateBy { it.key }

    return TaskConstraintEnum.values().map {
      val customization = constraintCustomizations[it]
      when (customization == null) {
        true -> TaskConstraintDto(key = it, active = it.isStandard)
        else -> TaskConstraintDto(key = it, active = customization.active, customization.name)
      }
    }
  }

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByIdentifier(identifier: UUID): TaskConstraintCustomization? =
      constraintCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@taskConstraintAuthorizationComponent.hasViewPermissionOnConstraint(#projectIdentifier)")
  open fun resolveProjectConstraints(
      projectIdentifier: ProjectId
  ): Map<TaskConstraintEnum, String> =
      findAll(projectIdentifier).associate {
        it.key to
            (it.name
                ?: messageSource.getMessage(
                    "TaskConstraintEnum_" + it.key.name, null, LocaleContextHolder.getLocale()))
      }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteConstraintCustomizationsByProjectIdendtifier(projectIdentifier: ProjectId) {
    constraintCustomizationRepository.findAllByProjectIdentifier(projectIdentifier).also {
      constraintCustomizationRepository.deleteAllInBatch(it)
    }
  }

  private fun assertAtLeastOneActiveConstraintRemains(
      projectIdentifier: ProjectId,
      constraintDto: UpdateTaskConstraintDto
  ) {
    if (constraintDto.active) return
    val activeConstraints = findAll(projectIdentifier).filter { it.active }.associateBy { it.key }
    if (activeConstraints.size <= 1 && activeConstraints.containsKey(constraintDto.key))
        throw PreconditionViolationException(
            Key.TASK_CONSTRAINT_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE)
  }
}
