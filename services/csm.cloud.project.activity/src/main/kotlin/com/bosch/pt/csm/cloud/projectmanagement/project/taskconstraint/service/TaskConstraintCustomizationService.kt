/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.TaskConstraintCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskConstraintCustomizationService(
    private val constraintCustomizationRepository: TaskConstraintCustomizationRepository
) {

  @Trace
  fun save(constraintCustomization: TaskConstraintCustomization) =
      constraintCustomizationRepository.save(constraintCustomization)

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID) =
      constraintCustomizationRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      constraintCustomizationRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      constraintCustomizationRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      constraintCustomizationRepository.deleteByVersion(identifier, version, projectIdentifier)

  @Trace
  fun findLatestCachedByProjectIdentifierAndKey(
      projectIdentifier: UUID,
      key: TaskConstraintEnum
  ): TaskConstraintCustomization? =
      constraintCustomizationRepository.findLatestCachedByProjectIdentifierAndKey(
          projectIdentifier, key)
}
