/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.daycard.command.service.DayCardService
import com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition.DayCardPrecondition.validateDeleteDayCardPossible
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshotStore
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class DeleteDayCardCommandHandler(
    private val dayCardService: DayCardService,
    private val eventBus: ProjectContextLocalEventBus,
    private val snapshotStore: DayCardSnapshotStore,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@dayCardAuthorizationComponent.hasContributePermissionOnDayCard(#command.identifier)")
  open fun handle(command: DeleteDayCardCommand) {
    val dayCard = dayCardCommandHandlerHelper.findDayCardOrFail(command.identifier)

    dayCardService.removeDayCardsFromTaskSchedule(
        dayCard.identifier, dayCard.taskSchedule.identifier, command.scheduleETag)
    deleteDayCard(dayCard)
  }

  private fun deleteDayCard(dayCard: DayCard) {
    validateDeleteDayCardPossible(dayCard.status)

    snapshotStore.findOrFail(dayCard.identifier).toCommandHandler().emitEvent(DELETED).to(eventBus)
  }
}
