/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CancelDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.CancelDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardIds
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardAuthorizationDto
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CancelDayCardBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val cancelDayCardCommandHandler: CancelDayCardCommandHandler,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(
      versionedIdentifiers: Collection<VersionedIdentifier>,
      reason: DayCardReasonEnum
  ) {
    if (versionedIdentifiers.isEmpty()) {
      return
    }

    val dayCards =
        dayCardCommandHandlerHelper.findDayCardsOrFail(
            VersionedIdentifier.mapToSetOfIds(versionedIdentifiers).asDayCardIds())
    dayCardCommandHandlerHelper.authorizeCancellation(
        dayCards.map { DayCardAuthorizationDto(it.identifier, it.status) }.toSet())

    businessTransactionManager.doBatchInBusinessTransaction(dayCards.projectIdentifier()) {
      dayCards.map {
        cancelDayCardCommandHandler.handle(
            CancelDayCardCommand(
                identifier = it.identifier,
                reason = reason,
                eTag = dayCardCommandHandlerHelper.getEtag(it, versionedIdentifiers)))
      }
    }
  }

  private fun Set<DayCard>.projectIdentifier() = this.first().taskSchedule.project.identifier
}
