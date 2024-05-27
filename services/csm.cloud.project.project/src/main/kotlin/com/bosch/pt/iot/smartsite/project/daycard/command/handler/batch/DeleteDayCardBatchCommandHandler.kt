/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardsFromScheduleCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.DeleteDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteDayCardBatchCommandHandler(
    private val deleteDayCardCommandHandler: DeleteDayCardCommandHandler,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun handle(commands: List<DeleteDayCardsFromScheduleCommand>, projectIdentifier: ProjectId) =
      businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
        commands.forEach {
          if (it.identifiers.isNotEmpty()) {
            val dayCards = dayCardCommandHandlerHelper.findDayCardsOrFail(it.identifiers.toSet())
            dayCardCommandHandlerHelper.assertDayCardsBelongToSameTask(dayCards)

            var currentScheduleETag = it.scheduleETag.toVersion()

            it.identifiers.map { identifier ->
              deleteDayCardCommandHandler.handle(
                  DeleteDayCardCommand(identifier, ETag.from(currentScheduleETag++)))
            }
          }
        }
      }
}
