/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.RemoveMilestoneFromListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class RemoveMilestoneFromListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize // this is an internal command not exposed via any controller
  open fun handle(command: RemoveMilestoneFromListCommand) =
      snapshotStore
          .findOrFail(command.milestoneListRef)
          .apply {
            requireContainsMilestone(command.identifier)
            checkNotEmptyAfterRemovingMilestone(command.identifier)
          }
          .toCommandHandler()
          .update {
            it.copy(
                milestoneRefs =
                    it.milestoneRefs.also { milestoneList ->
                      milestoneList.remove(command.identifier)
                    })
          }
          .emitEvent(ITEMREMOVED)
          .to(eventBus)

  private fun MilestoneListSnapshot.requireContainsMilestone(milestoneRef: MilestoneId) =
      require(milestoneRefs.contains(milestoneRef)) {
        "Cannot remove milestone from list. " +
            "Milestone $milestoneRef does not belong to " +
            "MilestoneList $identifier"
      }

  private fun MilestoneListSnapshot.checkNotEmptyAfterRemovingMilestone(
      milestoneIdentifier: MilestoneId
  ) =
      check(milestoneRefs.size > 1) {
        "Cannot remove milestone $milestoneIdentifier from list $identifier because it would be empty " +
            "after removing the milestone. Delete the list instead."
      }
}
