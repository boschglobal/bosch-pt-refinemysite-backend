/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextLocalEventBus
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.RemoveDayCardsFromTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshotStore
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class RemoveDayCardFromTaskScheduleCommandHandler(
    private val snapshotStore: TaskScheduleSnapshotStore,
    private val eventBus: ProjectContextLocalEventBus
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@dayCardAuthorizationComponent.hasContributePermissionOnDayCard(#command.identifier)")
  open fun handle(command: RemoveDayCardsFromTaskScheduleCommand) {
    val schedule = snapshotStore.findOrFail(command.taskScheduleIdentifier)
    ETag.from(command.scheduleETag.toVersion()).verify(schedule.version)

    val newSlots = mutableListOf<TaskScheduleSlotDto>()
    newSlots.addAll(schedule.slots ?: listOf())

    newSlots.removeIf { requireNotNull(it.id) == command.identifier }

    snapshotStore
        .findOrFail(schedule.identifier)
        .toCommandHandler()
        .update { it.copy(slots = newSlots) }
        .emitEvent(TaskScheduleEventEnumAvro.UPDATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .andReturnSnapshot()
        .identifier
  }
}
