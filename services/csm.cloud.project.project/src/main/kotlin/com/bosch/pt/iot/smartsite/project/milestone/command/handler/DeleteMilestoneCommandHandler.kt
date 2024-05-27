/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneListService
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshotStore
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteMilestoneCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneSnapshotStore,
    private val milestoneListService: MilestoneListService
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasUpdateAndDeletePermissionOnMilestone(" +
          "#command.identifier)")
  open fun handle(command: DeleteMilestoneCommand) {
    val milestoneSnapshot = snapshotStore.findOrFail(command.identifier)

    // remove milestone from list or delete list
    milestoneListService.removeMilestoneFromList(milestoneSnapshot)

    milestoneSnapshot
        .toCommandHandler()
        .assertVersionMatches(command.version)
        .emitEvent(DELETED)
        .to(eventBus)
  }
}
