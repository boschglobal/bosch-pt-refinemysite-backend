/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.ReorderMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ReorderMilestoneListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize // this is an internal command not exposed via any controller
  open fun handle(command: ReorderMilestoneListCommand) {
    val milestoneListSnapshot = snapshotStore.findOrFail(command.identifier)
    val position = command.adjustPositionIfRequired(milestoneListSnapshot)

    if (milestoneListSnapshot.shouldReorderMilestone(command.milestoneRef, position)) {
      milestoneListSnapshot
          .toCommandHandler()
          .update {
            it.copy(
                milestoneRefs =
                    it.milestoneRefs.also { milestoneList ->
                      milestoneList.remove(command.milestoneRef)
                      milestoneList.add(position, command.milestoneRef)
                    })
          }
          .emitEvent(REORDERED)
          .ifSnapshotWasChanged()
          .to(eventBus)
    }
  }

  private fun ReorderMilestoneListCommand.adjustPositionIfRequired(list: MilestoneListSnapshot) =
      when {
        position < 0 -> 0
        position >= list.milestoneRefs.size -> list.milestoneRefs.size
        else -> position
      }

  private fun MilestoneListSnapshot.shouldReorderMilestone(
      milestoneRef: MilestoneId,
      position: Int
  ): Boolean = milestoneRefs[position] != milestoneRef
}
