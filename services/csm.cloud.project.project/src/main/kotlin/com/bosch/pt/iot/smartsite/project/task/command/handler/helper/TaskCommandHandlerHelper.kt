/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.handler.helper

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_WORK_AREA_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import org.springframework.stereotype.Component

@Component
class TaskCommandHandlerHelper(
    private val projectRepository: ProjectRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository
) {
  fun returnProjectCraftIfExists(
      projectCraftIdentifier: ProjectCraftId,
      projectIdentifier: ProjectId
  ): ProjectCraftId =
      if (projectCraftRepository.existsByIdentifierAndProjectIdentifier(
          projectCraftIdentifier, projectIdentifier)) {
        projectCraftIdentifier
      } else {
        throw PreconditionViolationException(TASK_VALIDATION_ERROR_CRAFT_NOT_FOUND)
      }

  fun returnWorkAreaIfExistsAndRequired(
      workAreaIdentifier: WorkAreaId?,
      projectIdentifier: ProjectId
  ): WorkAreaId? =
      if (workAreaIdentifier != null) {
        if (workAreaRepository.existsByIdentifierAndProjectIdentifier(
            workAreaIdentifier, projectIdentifier)) {
          workAreaIdentifier
        } else {
          throw PreconditionViolationException(TASK_VALIDATION_ERROR_WORK_AREA_NOT_FOUND)
        }
      } else {
        null
      }

  fun returnProjectIfExistsAndRequired(projectIdentifier: ProjectId): ProjectId =
      if (projectRepository.existsByIdentifier(projectIdentifier)) {
        projectIdentifier
      } else {
        throw PreconditionViolationException(TASK_VALIDATION_ERROR_PROJECT_NOT_FOUND)
      }

  fun assertTasksBelongToSameProject(projectIdentifiers: Collection<ProjectId>) {
    if (projectIdentifiers.distinct().count() > 1) {
      throw PreconditionViolationException(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)
    }
  }
}
