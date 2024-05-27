/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.mapper.TaskScheduleAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import java.time.LocalDate
import java.time.LocalDateTime

data class TaskScheduleSnapshot(
    override val identifier: TaskScheduleId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectIdentifier: ProjectId,
    val start: LocalDate?,
    val end: LocalDate?,
    val taskIdentifier: TaskId?,
    val slots: Collection<TaskScheduleSlotDto>?
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      taskSchedule: TaskSchedule
  ) : this(
      taskSchedule.identifier,
      taskSchedule.version,
      taskSchedule.createdBy.get(),
      taskSchedule.createdDate.get(),
      taskSchedule.lastModifiedBy.get(),
      taskSchedule.lastModifiedDate.get(),
      taskSchedule.project.identifier,
      taskSchedule.start,
      taskSchedule.end,
      taskSchedule.task.identifier,
      taskSchedule.slots?.map { TaskScheduleSlotDto(it.dayCard.identifier, it.date) })

  fun toCommandHandler() = CommandHandler.of(this, TaskScheduleAvroSnapshotMapper)
}

fun TaskSchedule.asSnapshot() = TaskScheduleSnapshot(this)
