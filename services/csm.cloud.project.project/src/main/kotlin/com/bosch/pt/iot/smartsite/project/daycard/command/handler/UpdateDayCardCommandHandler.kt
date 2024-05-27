/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.project.daycard.command.api.UpdateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshot
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshotStore
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class UpdateDayCardCommandHandler(
    private val snapshotStore: DayCardSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@dayCardAuthorizationComponent.hasContributePermissionOnDayCard(#command.identifier)")
  open fun handle(command: UpdateDayCardCommand) {
    snapshotStore
        .findOrFail(command.identifier)
        .apply { validateDayCardStatus(this) }
        .toCommandHandler()
        .assertVersionMatches(command.eTag.toVersion())
        .update {
          it.copy(title = command.title, manpower = command.manpower, notes = command.notes)
        }
        .emitEvent(UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun validateDayCardStatus(dayCard: DayCardSnapshot) {
    if (dayCard.status != OPEN) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
    }
  }
}
