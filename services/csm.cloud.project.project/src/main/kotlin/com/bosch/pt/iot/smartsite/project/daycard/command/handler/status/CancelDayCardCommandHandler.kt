/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.status

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.RFV_VALIDATION_ERROR_REASON_DEACTIVATED
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CancelDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshotStore
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.NOTDONE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardAuthorizationDto
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CancelDayCardCommandHandler(
    private val snapshotStore: DayCardSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus,
    private val rfvService: RfvService,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize(usedByController = true)
  open fun handle(command: CancelDayCardCommand) {
    val dayCardSnapshot = snapshotStore.findOrFail(command.identifier)

    dayCardCommandHandlerHelper.authorizeCancellation(
        listOf(dayCardSnapshot).map { DayCardAuthorizationDto(it.identifier, it.status!!) }.toSet())

    val rfvs = rfvService.findAll(dayCardSnapshot.projectIdentifier)

    val customizedRfv = rfvs.firstOrNull { it.key === command.reason }
    if (customizedRfv != null && !customizedRfv.active) {
      throw PreconditionViolationException(RFV_VALIDATION_ERROR_REASON_DEACTIVATED)
    }

    snapshotStore
        .findOrFail(command.identifier)
        .toCommandHandler()
        .assertVersionMatches(command.eTag.toVersion())
        .update { it.copy(status = NOTDONE, reason = command.reason) }
        .emitEvent(CANCELLED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
