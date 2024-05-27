/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.mapper.ProjectCraftListAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import java.time.LocalDateTime

data class ProjectCraftListSnapshot(
    override val identifier: ProjectCraftListId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectIdentifier: ProjectId,
    val projectCraftIdentifiers: MutableList<ProjectCraftId> = mutableListOf()
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      projectCraftList: ProjectCraftList
  ) : this(
      projectCraftList.identifier,
      projectCraftList.version,
      projectCraftList.createdBy.get(),
      projectCraftList.createdDate.get(),
      projectCraftList.lastModifiedBy.get(),
      projectCraftList.lastModifiedDate.get(),
      projectCraftList.project.identifier,
      projectCraftList.projectCrafts.map { it.identifier }.toMutableList())

  fun toCommandHandler() = CommandHandler.of(this, ProjectCraftListAvroSnapshotMapper)
}

fun ProjectCraftList.asSnapshot() = ProjectCraftListSnapshot(this)
