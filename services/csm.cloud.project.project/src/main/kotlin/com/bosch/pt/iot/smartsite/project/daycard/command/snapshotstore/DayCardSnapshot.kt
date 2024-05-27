/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.daycard.command.mapper.DayCardAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.math.BigDecimal
import java.time.LocalDateTime

data class DayCardSnapshot(
    override val identifier: DayCardId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectIdentifier: ProjectId,
    val taskScheduleIdentifier: TaskScheduleId?,
    val taskIdentifier: TaskId?,
    val title: String?,
    val manpower: BigDecimal,
    val notes: String?,
    val status: DayCardStatusEnum?,
    val reason: DayCardReasonEnum? = null
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      dayCard: DayCard
  ) : this(
      dayCard.identifier,
      dayCard.version,
      dayCard.createdBy.get(),
      dayCard.createdDate.get(),
      dayCard.lastModifiedBy.get(),
      dayCard.lastModifiedDate.get(),
      dayCard.taskSchedule.project.identifier,
      dayCard.taskSchedule.identifier,
      dayCard.taskSchedule.task.identifier,
      dayCard.title,
      dayCard.manpower,
      dayCard.notes,
      dayCard.status,
      dayCard.reason)

  fun toCommandHandler() = CommandHandler.of(this, DayCardAvroSnapshotMapper)
}

fun DayCard.asSnapshot() = DayCardSnapshot(this)
