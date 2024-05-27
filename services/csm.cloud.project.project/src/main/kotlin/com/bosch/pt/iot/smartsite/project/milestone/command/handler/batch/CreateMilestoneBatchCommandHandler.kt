/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.CreateMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateMilestoneBatchCommandHandler(
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val createMilestoneCommandHandler: CreateMilestoneCommandHandler
) {

  @Trace
  @Transactional
  @NoPreAuthorize // authorization is handled by non-batch command handlers
  open fun handle(
      projectRef: ProjectId,
      commands: List<CreateMilestoneCommand>
  ): List<MilestoneId> {
    if (commands.isEmpty()) return emptyList()

    // Check constraints and permissions
    require(commands.allBelongToSameProject()) {
      "Multiple milestones can only be created for one project at at time"
    }
    val projectIdentifierFromCommands = commands.first().projectRef

    require(projectIdentifierFromCommands == projectRef) {
      "Milestones cannot be created for a foreign project"
    }

    // Populate existence cache
    projectCraftRepository.findAllByProjectIdentifier(projectIdentifierFromCommands)
    workAreaRepository.findAllByIdentifierIn(
        commands.mapNotNull(CreateMilestoneCommand::workAreaRef).distinct())

    return commands.sortedBy { it.position }.map { createMilestoneCommandHandler.handle(it) }
  }

  private fun List<CreateMilestoneCommand>.allBelongToSameProject(): Boolean =
      map { it.projectRef }.distinct().size == 1
}
