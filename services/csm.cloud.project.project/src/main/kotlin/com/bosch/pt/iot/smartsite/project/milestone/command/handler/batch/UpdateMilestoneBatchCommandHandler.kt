/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.milestone.command.api.UpdateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.UpdateMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateMilestoneBatchCommandHandler(
    private val snapshotStore: MilestoneSnapshotStore,
    private val updateMilestoneCommandHandler: UpdateMilestoneCommandHandler,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val milestoneRepository: MilestoneRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  @Trace
  @Transactional(propagation = MANDATORY)
  @NoPreAuthorize
  open fun handle(
      projectRef: ProjectId,
      commands: List<UpdateMilestoneCommand>
  ): List<MilestoneId> {
    if (commands.isEmpty()) return emptyList()

    // Check constraints and permissions
    require(commands.allBelongToProject(projectRef)) {
      "Could not find one or more milestones in project $projectRef. " +
          "Either they belong to a different project or have been deleted."
    }

    // Populate existence cache
    projectCraftRepository.findAllByProjectIdentifier(projectRef)
    workAreaRepository.findAllByIdentifierIn(
        commands.mapNotNull(UpdateMilestoneCommand::workAreaRef).distinct())

    // Populate snapshot cache
    snapshotStore.findAllOrIgnore(commands.map { it.identifier }.distinct())

    return businessTransactionManager.doBatchInBusinessTransaction(projectRef) {
      commands.sortedBy { it.position }.map { updateMilestoneCommandHandler.handle(it) }
    }
  }

  private fun List<UpdateMilestoneCommand>.allBelongToProject(projectRef: ProjectId): Boolean =
      milestoneRepository.existsByIdentifierInAndProjectIdentifier(
          map { it.identifier }.toSet(), projectRef)
}
