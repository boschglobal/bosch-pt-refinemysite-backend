/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_CRAFT_MISSING
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_WORK_AREA_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneListService
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateMilestoneCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val milestoneListService: MilestoneListService
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasCreateMilestonePermissionOnProject(" +
          "#command.projectRef, #command.type)")
  open fun handle(command: CreateMilestoneCommand): MilestoneId {
    val milestoneSnapshot =
        MilestoneSnapshot(
                identifier = command.identifier,
                version = INITIAL_SNAPSHOT_VERSION,
                name = command.name,
                type = command.type,
                date = command.date,
                header = command.header,
                projectRef = command.projectRef,
                craftRef = command.returnProjectCraftIfExistsAndRequired(),
                workAreaRef = command.returnWorkAreaIfExistsAndRequired(),
                description = command.description)
            .toCommandHandler()
            .emitEvent(CREATED)
            .to(eventBus)
            .andReturnSnapshot()

    // add milestone to corresponding list or create new list if there is none yet
    milestoneListService.addMilestoneToList(milestoneSnapshot, command.position)

    return milestoneSnapshot.identifier
  }

  private fun CreateMilestoneCommand.returnProjectCraftIfExistsAndRequired(): ProjectCraftId? =
      if (type == CRAFT) {
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

  private fun CreateMilestoneCommand.returnWorkAreaIfExistsAndRequired(): WorkAreaId? =
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
