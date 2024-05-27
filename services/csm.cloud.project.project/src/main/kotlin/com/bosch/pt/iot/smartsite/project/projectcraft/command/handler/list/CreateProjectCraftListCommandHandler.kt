/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.list

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftListCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftListSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateProjectCraftListCommandHandler(private val eventBus: ProjectContextLocalEventBus) {
  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(command: CreateProjectCraftListCommand) =
      ProjectCraftListSnapshot(
              identifier = ProjectCraftListId(),
              version = INITIAL_SNAPSHOT_VERSION,
              projectIdentifier = command.projectIdentifier)
          .toCommandHandler()
          .emitEvent(CREATED)
          .to(eventBus)
}
