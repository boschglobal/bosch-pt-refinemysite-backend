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
import com.bosch.pt.iot.smartsite.project.projectcraft.command.mapper.ProjectCraftAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import java.time.LocalDateTime

data class ProjectCraftSnapshot(
    override val identifier: ProjectCraftId,
    override val version: Long,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectIdentifier: ProjectId,
    val name: String,
    val color: String
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      projectCraft: ProjectCraft
  ) : this(
      projectCraft.identifier,
      projectCraft.version,
      projectCraft.createdBy.get(),
      projectCraft.createdDate.get(),
      projectCraft.lastModifiedBy.get(),
      projectCraft.lastModifiedDate.get(),
      projectCraft.project.identifier,
      projectCraft.name,
      projectCraft.color)

  fun toCommandHandler() = CommandHandler.of(this, ProjectCraftAvroSnapshotMapper)
}

fun ProjectCraft.asSnapshot() = ProjectCraftSnapshot(this)
