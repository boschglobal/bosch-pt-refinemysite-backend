/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.list

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMADDED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.AddMilestoneToListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class AddMilestoneToListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneListSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize // this is an internal command not exposed via any controller
  open fun handle(command: AddMilestoneToListCommand): MilestoneListId {
    return snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .checkPrecondition { !it.milestoneRefs.contains(command.milestoneRef) }
        .onFailureThrow {
          error(
              "Milestone ${command.milestoneRef} is already contained in milestone list ${command.identifier}")
        }
        .update {
          it.copy(
              milestoneRefs =
                  it.milestoneRefs.also { milestoneRefs ->
                    milestoneRefs.add(
                        adjustPositionIfRequired(milestoneRefs, command.position),
                        command.milestoneRef)
                  })
        }
        .emitEvent(ITEMADDED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun adjustPositionIfRequired(milestoneList: List<*>, position: Int) =
      when {
        position < 0 -> 0
        position >= milestoneList.size -> milestoneList.size
        else -> position
      }
}
