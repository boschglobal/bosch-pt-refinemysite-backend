/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler

import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.helper.DayCardCommandHandlerHelper
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshot
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CreateDayCardCommandHandler(
    private val eventBus: ProjectContextLocalEventBus,
    private val dayCardCommandHandlerHelper: DayCardCommandHandlerHelper
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@taskAuthorizationComponent.hasContributePermissionOnTask(#command.taskIdentifier)")
  open fun handle(command: CreateDayCardCommand): DayCardId {
    val taskSchedule = dayCardCommandHandlerHelper.findTaskScheduleOrFail(command.taskIdentifier)

    dayCardCommandHandlerHelper.checkProjectExistsOrFail(taskSchedule.project.identifier)

    return DayCardSnapshot(
            identifier = command.identifier,
            version = INITIAL_SNAPSHOT_VERSION,
            projectIdentifier = taskSchedule.project.identifier,
            taskScheduleIdentifier = taskSchedule.identifier,
            taskIdentifier = command.taskIdentifier,
            title = command.title,
            manpower = command.manpower,
            notes = command.notes,
            status = command.status ?: DayCardStatusEnum.OPEN,
            reason = command.reason)
        .toCommandHandler()
        .emitEvent(CREATED)
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
