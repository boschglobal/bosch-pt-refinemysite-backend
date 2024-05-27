/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.command.api.UpdateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshotStore
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateWorkAreaCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: WorkAreaSnapshotStore,
    private val workAreaRepository: WorkAreaRepository,
    private val projectRepository: ProjectRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@workAreaAuthorizationComponent.hasUpdatePermissionOnWorkArea(#command.identifier)")
  open fun handle(command: UpdateWorkAreaCommand): WorkAreaId {
    val oldWorkAreaSnapshot = snapshotStore.findOrFail(command.identifier)

    isNameDifferent(oldWorkAreaSnapshot, command.name)

    val newWorkAreaSnapshot =
        oldWorkAreaSnapshot
            .toCommandHandler()
            .assertVersionMatches(command.version)
            .update { it.copy(name = command.name) }
            .emitEvent(UPDATED)
            .ifSnapshotWasChanged()
            .to(eventBus)
            .andReturnSnapshot()

    return newWorkAreaSnapshot.identifier
  }

  private fun isNameDifferent(workArea: WorkAreaSnapshot, updateName: String) {
    val projectId = findProjectIdOrFail(workArea.projectRef)
    val differentName = !updateName.equals(workArea.name, ignoreCase = true)
    if (differentName &&
        workAreaRepository.existsByNameIgnoreCaseAndProjectIdAndParent(
            updateName, projectId, workArea.parentRef)) {
      throw PreconditionViolationException(WORK_AREA_VALIDATION_ERROR_USED_NAME)
    }
  }

  private fun findProjectIdOrFail(projectId: ProjectId): Long =
      requireNotNull(projectRepository.findIdByIdentifier(projectId)) {
        "Could not find Project $projectId"
      }
}
