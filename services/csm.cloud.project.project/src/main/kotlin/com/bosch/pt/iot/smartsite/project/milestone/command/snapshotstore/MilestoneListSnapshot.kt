/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.command.mapper.MilestoneListAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import java.time.LocalDateTime

data class MilestoneListSnapshot(
    override val identifier: MilestoneListId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectRef: ProjectId,
    val date: LocalDate,
    val header: Boolean,
    val workAreaRef: WorkAreaId?,
    val milestoneRefs: MutableList<MilestoneId> = mutableListOf()
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      milestoneList: MilestoneList
  ) : this(
      milestoneList.identifier,
      milestoneList.version,
      milestoneList.createdBy.get(),
      milestoneList.createdDate.get(),
      milestoneList.lastModifiedBy.get(),
      milestoneList.lastModifiedDate.get(),
      milestoneList.project.identifier,
      milestoneList.date,
      milestoneList.header,
      milestoneList.workArea?.identifier,
      milestoneList.milestones.map { it.identifier }.toMutableList())

  fun toCommandHandler() = CommandHandler.of(this, MilestoneListAvroSnapshotMapper)
}

fun MilestoneList.asSnapshot() = MilestoneListSnapshot(this)
