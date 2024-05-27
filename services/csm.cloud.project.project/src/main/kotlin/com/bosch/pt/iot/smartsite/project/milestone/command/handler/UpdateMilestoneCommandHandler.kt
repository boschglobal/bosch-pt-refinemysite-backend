/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_CRAFT_MISSING
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_WORK_AREA_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.UpdateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneListService
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateMilestoneCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: MilestoneSnapshotStore,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val milestoneListService: MilestoneListService
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasUpdateAndDeletePermissionOnMilestone(" +
          "#command.identifier)")
  open fun handle(command: UpdateMilestoneCommand): MilestoneId {

    val oldMilestoneSnapshot = snapshotStore.findOrFail(command.identifier)

    val newMilestoneSnapshot =
        oldMilestoneSnapshot
            .toCommandHandler()
            .assertVersionMatches(command.version)
            .update {
              it.copy(
                  name = command.name,
                  type = command.type,
                  date = command.date,
                  header = command.header,
                  craftRef = command.returnProjectCraftIfExistsAndRequired(it.projectRef),
                  workAreaRef = command.returnWorkAreaIfExistsAndRequired(it.projectRef),
                  description = command.description)
            }
            .emitEvent(UPDATED)
            .ifSnapshotWasChanged()
            .to(eventBus)
            .andReturnSnapshot()

    // move milestone to other list, if needed, and update milestone position inside the list
    milestoneListService.updateListAfterMilestoneUpdate(
        oldMilestoneSnapshot, newMilestoneSnapshot, command.position)

    return newMilestoneSnapshot.identifier
  }

  private fun UpdateMilestoneCommand.returnProjectCraftIfExistsAndRequired(
      projectRef: ProjectId
  ): ProjectCraftId? =
      if (type == MilestoneTypeEnum.CRAFT) {
        when (craftRef) {
          null -> throw PreconditionViolationException(MILESTONE_VALIDATION_ERROR_CRAFT_MISSING)
          else ->
              if (projectCraftRepository.existsByIdentifierAndProjectIdentifier(
                  craftRef, projectRef)) {
                craftRef
              } else {
                throw PreconditionViolationException(MILESTONE_VALIDATION_ERROR_CRAFT_NOT_FOUND)
              }
        }
      } else {
        null
      }

  private fun UpdateMilestoneCommand.returnWorkAreaIfExistsAndRequired(
      projectRef: ProjectId
  ): WorkAreaId? =
      if (!header && workAreaRef != null) {
        if (workAreaRepository.existsByIdentifierAndProjectIdentifier(workAreaRef, projectRef)) {
          workAreaRef
        } else {
          throw PreconditionViolationException(MILESTONE_VALIDATION_ERROR_WORK_AREA_NOT_FOUND)
        }
      } else {
        null
      }
}
