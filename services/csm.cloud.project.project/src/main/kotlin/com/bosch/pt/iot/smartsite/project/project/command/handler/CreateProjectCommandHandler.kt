/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.ProjectSnapshot
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateProjectCommandHandler(private val eventBus: ProjectContextLocalEventBus) {

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasCreatePermissionOnProject()")
  open fun handle(command: CreateProjectCommand): ProjectId =
      ProjectSnapshot(
              identifier = command.identifier,
              version = INITIAL_SNAPSHOT_VERSION,
              client = command.client,
              description = command.description,
              start = command.start,
              end = command.end,
              projectNumber = command.projectNumber,
              title = command.title,
              category = command.category,
              address = command.address)
          .toCommandHandler()
          .emitEvent(CREATED)
          .to(eventBus)
          .andReturnSnapshot()
          .identifier
}
