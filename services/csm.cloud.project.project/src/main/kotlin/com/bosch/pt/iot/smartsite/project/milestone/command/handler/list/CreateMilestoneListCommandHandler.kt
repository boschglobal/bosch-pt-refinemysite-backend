/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler.list

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneListCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateMilestoneListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val workAreaRepository: WorkAreaRepository,
    private val milestoneSnapshotStore: MilestoneSnapshotStore
) {

  @Trace
  @Transactional
  @NoPreAuthorize // this is an internal command not exposed via any controller
  open fun handle(command: CreateMilestoneListCommand): MilestoneListId {
    with(command) {
      checkMilestoneExistsInProject()
      checkHeaderAndWorkAreaAreNotBothSet()
      checkWorkAreaExistsIfProvided()
    }
    return MilestoneListSnapshot(
            identifier = MilestoneListId(),
            version = INITIAL_SNAPSHOT_VERSION,
            projectRef = command.projectRef,
            date = command.date,
            header = command.header,
            workAreaRef = command.workAreaRef,
            milestoneRefs = mutableListOf(command.milestoneRef))
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun CreateMilestoneListCommand.checkMilestoneExistsInProject() =
      check(milestoneSnapshotStore.findOrIgnore(milestoneRef)?.projectRef == projectRef) {
        "Milestone $milestoneRef could not be found in project $projectRef."
      }

  private fun CreateMilestoneListCommand.checkHeaderAndWorkAreaAreNotBothSet() {
    if (header && workAreaRef != null) {
      error("When header is true, the work area must not be set.")
    }
  }

  private fun CreateMilestoneListCommand.checkWorkAreaExistsIfProvided() {
    if (!header && workAreaRef != null) {
      check(workAreaRepository.existsByIdentifierAndProjectIdentifier(workAreaRef, projectRef)) {
        "Could not find work area $workAreaRef"
      }
    }
  }
}
