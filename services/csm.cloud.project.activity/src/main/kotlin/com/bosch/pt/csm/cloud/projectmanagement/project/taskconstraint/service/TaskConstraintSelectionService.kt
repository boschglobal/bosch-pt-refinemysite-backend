/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.service

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.repository.TaskConstraintSelectionRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TaskConstraintSelectionService(
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository
) {

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID): TaskConstraintSelection? =
      taskConstraintSelectionRepository.find(identifier, version, projectIdentifier)

  @Trace
  fun save(taskConstraintSelection: TaskConstraintSelection) =
      taskConstraintSelectionRepository.save(taskConstraintSelection)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      taskConstraintSelectionRepository.deleteByVersion(identifier, version, projectIdentifier)
}
