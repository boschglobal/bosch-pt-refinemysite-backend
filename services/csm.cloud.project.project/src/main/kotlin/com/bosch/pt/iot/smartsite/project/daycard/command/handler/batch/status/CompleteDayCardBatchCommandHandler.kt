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
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CompleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.CompleteDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardIds
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CompleteDayCardBatchCommandHandler(
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val completeDayCardCommandHandler: CompleteDayCardCommandHandler,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(versionedIdentifiers: Collection<VersionedIdentifier>) {
    if (versionedIdentifiers.isEmpty()) {
      return
    }

    val dayCards =
        dayCardCommandHandlerHelper.findDayCardsOrFail(
            VersionedIdentifier.mapToSetOfIds(versionedIdentifiers).asDayCardIds())

    businessTransactionManager.doBatchInBusinessTransaction(dayCards.projectIdentifier()) {
      dayCards.map {
        completeDayCardCommandHandler.handle(
            CompleteDayCardCommand(
                identifier = it.identifier,
                eTag = dayCardCommandHandlerHelper.getEtag(it, versionedIdentifiers)))
      }
    }
  }

  private fun Set<DayCard>.projectIdentifier() = this.first().taskSchedule.project.identifier
}
