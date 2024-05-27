/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.project.daycard.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore.TaskScheduleSnapshot
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import java.time.ZoneOffset.UTC
import java.util.UUID

object TaskScheduleAvroSnapshotMapper : AbstractAvroSnapshotMapper<TaskScheduleSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: TaskScheduleSnapshot,
      eventType: E
  ): TaskScheduleEventAvro =
      with(snapshot) {
        TaskScheduleEventAvro.newBuilder()
            .setName((eventType as TaskScheduleEventEnumAvro))
            .setAggregateBuilder(
                TaskScheduleAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setTask(requireNotNull(taskIdentifier).toAggregateReference())
                    .setStart(
                        if (start == null) null
                        else start.atStartOfDay(UTC).toInstant().toEpochMilli())
                    .setEnd(
                        if (end == null) null else end.atStartOfDay(UTC).toInstant().toEpochMilli())
                    .setSlots(
                        requireNotNull(slots).map { slot: TaskScheduleSlotDto ->
                          TaskScheduleSlotAvro.newBuilder()
                              .setDate(
                                  requireNotNull(slot.date)
                                      .atStartOfDay(UTC)
                                      .toInstant()
                                      .toEpochMilli())
                              .setDayCard(requireNotNull(slot.id).toAggregateReference())
                              .build()
                        }))
            .build()
      }

  override fun getAggregateType() = TASKSCHEDULE.value

  override fun getRootContextIdentifier(snapshot: TaskScheduleSnapshot): UUID =
      snapshot.projectIdentifier.toUuid()
}
