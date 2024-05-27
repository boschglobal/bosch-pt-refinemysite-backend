/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshotStore
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteMilestoneListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize // this is an internal command not exposed via any controller
  open fun handle(command: DeleteMilestoneListCommand) {
    snapshotStore
        .findOrFail(command.identifier)
        .apply { checkContainsExactlyOneMilestone() }
        .toCommandHandler()
        .emitEvent(DELETED)
        .to(eventBus)
  }

  private fun MilestoneListSnapshot.checkContainsExactlyOneMilestone() =
      check(milestoneRefs.size == 1) {
        "Cannot delete list $identifier because it still contains more than a single milestone. " +
            "First remove all milestones except one, then delete the list."
      }
}
