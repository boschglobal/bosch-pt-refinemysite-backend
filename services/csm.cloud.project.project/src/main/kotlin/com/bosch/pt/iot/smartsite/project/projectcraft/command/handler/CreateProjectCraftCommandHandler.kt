/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.service.ProjectCraftListService
import com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore.ProjectCraftSnapshot
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateProjectCraftCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val projectCraftListService: ProjectCraftListService,
    private val repository: ProjectCraftRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectAuthorizationComponent.hasUpdatePermissionOnProject(#command.projectIdentifier)")
  open fun handle(command: CreateProjectCraftCommand): ProjectCraftId {
    checkProjectCraftNameIsNotInUse(command)

    val projectCraftIdentifier =
        ProjectCraftSnapshot(
                identifier = command.identifier,
                version = INITIAL_SNAPSHOT_VERSION,
                projectIdentifier = command.projectIdentifier,
                name = command.name,
                color = command.color)
            .toCommandHandler()
            .emitEvent(CREATED)
            .to(eventBus)
            .andReturnSnapshot()
            .identifier

    projectCraftListService.addProjectCraftToList(
        projectIdentifier = command.projectIdentifier,
        projectCraftIdentifier = projectCraftIdentifier,
        projectCraftListVersion = command.projectCraftListVersion,
        position = command.position)

    return projectCraftIdentifier
  }

  private fun checkProjectCraftNameIsNotInUse(command: CreateProjectCraftCommand) {
    if (repository.existsByNameIgnoreCaseAndProjectIdentifier(
        command.name, command.projectIdentifier)) {
      throw PreconditionViolationException(PROJECT_CRAFT_VALIDATION_ERROR_USED_NAME)
    }
  }
}
