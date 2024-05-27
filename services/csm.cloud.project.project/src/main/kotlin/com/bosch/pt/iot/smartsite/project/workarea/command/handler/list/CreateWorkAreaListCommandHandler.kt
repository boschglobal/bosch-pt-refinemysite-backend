/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler.list

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaListCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateWorkAreaListCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: CreateWorkAreaListCommand): WorkAreaListId {
    return WorkAreaListSnapshot(
            identifier = command.identifier,
            version = INITIAL_SNAPSHOT_VERSION,
            projectRef = command.projectRef)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
