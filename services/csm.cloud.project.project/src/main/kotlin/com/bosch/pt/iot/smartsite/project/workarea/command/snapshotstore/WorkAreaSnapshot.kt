/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.mapper.WorkAreaAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import java.time.LocalDateTime

data class WorkAreaSnapshot(
    override val identifier: WorkAreaId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val name: String,
    val parentRef: WorkAreaId?,
    val projectRef: ProjectId,
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      workArea: WorkArea
  ) : this(
      workArea.identifier,
      workArea.version,
      workArea.createdBy.get(),
      workArea.createdDate.get(),
      workArea.lastModifiedBy.get(),
      workArea.lastModifiedDate.get(),
      workArea.name,
      workArea.parent,
      workArea.project.identifier,
  )

  fun toCommandHandler() = CommandHandler.of(this, WorkAreaAvroSnapshotMapper)
}

fun WorkArea.asSnapshot() = WorkAreaSnapshot(this)
