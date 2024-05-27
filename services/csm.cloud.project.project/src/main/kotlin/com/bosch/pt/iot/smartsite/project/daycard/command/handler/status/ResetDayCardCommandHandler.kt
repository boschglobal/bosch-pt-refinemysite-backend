/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.status

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_UPDATE_NOT_POSSIBLE
import com.bosch.pt.iot.smartsite.project.daycard.command.api.ResetDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition.DayCardPrecondition
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshotStore
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class ResetDayCardCommandHandler(
    private val snapshotStore: DayCardSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize("@dayCardAuthorizationComponent.hasReviewPermissionOnDayCard(#command.identifier)")
  open fun handle(command: ResetDayCardCommand) {
    val dayCard = snapshotStore.findOrFail(command.identifier)

    assertResetDayCardPossible(dayCard.status!!)

    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.eTag.toVersion())
        .update { it.copy(status = OPEN, reason = null) }
        .emitEvent(RESET)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun assertResetDayCardPossible(status: DayCardStatusEnum) {
    if (!DayCardPrecondition.isResetPossible(status)) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_UPDATE_NOT_POSSIBLE)
    }
  }
}
