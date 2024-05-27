/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.service

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.AddProjectCraftToListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.RemoveProjectCraftFromListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.ReorderProjectCraftListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list.AddProjectCraftToListCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list.RemoveProjectCraftFromListCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list.ReorderProjectCraftListCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import org.springframework.stereotype.Component

@Component
class ProjectCraftListService(
    private val addProjectCraftToListCommandHandler: AddProjectCraftToListCommandHandler,
    private val reorderProjectCraftListCommandHandler: ReorderProjectCraftListCommandHandler,
    private val removeProjectCraftFromListCommandHandler: RemoveProjectCraftFromListCommandHandler,
    private val projectCraftListRepository: ProjectCraftListRepository
) {

  fun addProjectCraftToList(
      projectIdentifier: ProjectId,
      projectCraftIdentifier: ProjectCraftId,
      projectCraftListVersion: Long,
      position: Int?
  ) {
    val projectCraftList = findProjectCraftList(projectIdentifier)

    addProjectCraftToListCommandHandler.handle(
        AddProjectCraftToListCommand(
            identifier = projectCraftIdentifier,
            projectCraftListIdentifier = projectCraftList.identifier,
            projectCraftListVersion = projectCraftListVersion,
            projectCraftListSize = projectCraftList.projectCrafts.size,
            position = position))
  }

  fun reorderProjectCraftList(
      projectIdentifier: ProjectId,
      projectCraftIdentifier: ProjectCraftId,
      projectCraftListVersion: Long,
      position: Int
  ) {
    val projectCraftList = findProjectCraftList(projectIdentifier)

    reorderProjectCraftListCommandHandler.handle(
        ReorderProjectCraftListCommand(
            identifier = projectCraftIdentifier,
            projectCraftListIdentifier = projectCraftList.identifier,
            projectCraftListVersion = projectCraftListVersion,
            position = position))
  }

  fun removeProjectCraftFromList(
      projectIdentifier: ProjectId,
      projectCraftIdentifier: ProjectCraftId,
  ) {
    val projectCraftList = findProjectCraftList(projectIdentifier)

    removeProjectCraftFromListCommandHandler.handle(
        RemoveProjectCraftFromListCommand(
            identifier = projectCraftIdentifier,
            projectCraftListIdentifier = projectCraftList.identifier))
  }

  private fun findProjectCraftList(projectIdentifier: ProjectId) =
      projectCraftListRepository.findOneWithDetailsByProjectIdentifier(projectIdentifier)
          ?: error("Could not find project craft list for the project $projectIdentifier")
}
