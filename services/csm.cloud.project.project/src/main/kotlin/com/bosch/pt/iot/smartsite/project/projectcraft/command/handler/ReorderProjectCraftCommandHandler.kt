/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler

import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.ReorderProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.service.ProjectCraftListService
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshotStore
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ReorderProjectCraftCommandHandler(
    private val snapshotStore: ProjectCraftSnapshotStore,
    private val projectCraftListService: ProjectCraftListService
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectCraftAuthorizationComponent.hasUpdatePermissionOnProjectCraft(#command.identifier)")
  open fun handle(command: ReorderProjectCraftCommand) {
    snapshotStore.findOrFail(command.identifier)

    projectCraftListService.reorderProjectCraftList(
        projectIdentifier = command.projectIdentifier,
        projectCraftIdentifier = command.identifier,
        projectCraftListVersion = command.projectCraftListVersion,
        position = command.position)
  }
}
