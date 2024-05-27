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
import com.bosch.pt.iot.smartsite.project.milestone.command.mapper.MilestoneAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate
import java.time.LocalDateTime

data class MilestoneSnapshot(
    override val identifier: MilestoneId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val name: String,
    var type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val projectRef: ProjectId,
    val craftRef: ProjectCraftId?,
    val workAreaRef: WorkAreaId?,
    val description: String?
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      milestone: Milestone
  ) : this(
      milestone.identifier,
      milestone.version,
      milestone.createdBy.get(),
      milestone.createdDate.get(),
      milestone.lastModifiedBy.get(),
      milestone.lastModifiedDate.get(),
      milestone.name,
      milestone.type,
      milestone.date,
      milestone.header,
      milestone.project.identifier,
      milestone.craft?.identifier,
      milestone.workArea?.identifier,
      milestone.description)

  fun toCommandHandler() = CommandHandler.of(this, MilestoneAvroSnapshotMapper)
}

fun Milestone.asSnapshot() = MilestoneSnapshot(this)
