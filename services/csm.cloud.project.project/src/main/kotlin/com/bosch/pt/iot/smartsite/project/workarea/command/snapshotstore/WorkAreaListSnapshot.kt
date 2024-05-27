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
import com.bosch.pt.iot.smartsite.project.workarea.command.mapper.WorkAreaListAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import java.time.LocalDateTime

data class WorkAreaListSnapshot(
    override val identifier: WorkAreaListId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectRef: ProjectId,
    val workAreaRefs: MutableList<WorkAreaId> = mutableListOf()
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      workAreaList: WorkAreaList
  ) : this(
      workAreaList.identifier,
      workAreaList.version,
      workAreaList.createdBy.get(),
      workAreaList.createdDate.get(),
      workAreaList.lastModifiedBy.get(),
      workAreaList.lastModifiedDate.get(),
      workAreaList.project.identifier,
      workAreaList.workAreas.map { it.identifier }.toMutableList())

  fun toCommandHandler() = CommandHandler.of(this, WorkAreaListAvroSnapshotMapper)
}

fun WorkAreaList.asSnapshot() = WorkAreaListSnapshot(this)
