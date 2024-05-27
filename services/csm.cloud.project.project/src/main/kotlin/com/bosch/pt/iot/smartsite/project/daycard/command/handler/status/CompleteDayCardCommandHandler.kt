/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.status

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.COMPLETED
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CompleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition.DayCardPrecondition
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshotStore
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CompleteDayCardCommandHandler(
    private val snapshotStore: DayCardSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@dayCardAuthorizationComponent.hasContributePermissionOnDayCard(#command.identifier)")
  open fun handle(command: CompleteDayCardCommand) {
    val dayCard = snapshotStore.findOrFail(command.identifier)

    assertCompleteDayCardPossible(dayCard.status!!)

    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.eTag.toVersion())
        .update { it.copy(status = DayCardStatusEnum.DONE) }
        .emitEvent(COMPLETED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }

  private fun assertCompleteDayCardPossible(status: DayCardStatusEnum) {
    if (!DayCardPrecondition.isCompletePossible(status)) {
      throw PreconditionViolationException(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
    }
  }
}
