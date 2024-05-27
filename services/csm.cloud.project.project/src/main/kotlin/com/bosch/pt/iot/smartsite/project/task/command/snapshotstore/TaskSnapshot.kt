/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.command.mapper.TaskAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDateTime

data class TaskSnapshot(
    override val identifier: TaskId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectIdentifier: ProjectId,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val projectCraftIdentifier: ProjectCraftId,
    val assigneeIdentifier: ParticipantId? = null,
    val workAreaIdentifier: WorkAreaId? = null,
    val status: TaskStatusEnum,
    val topics: List<TopicId>? = null,
    val editDate: LocalDateTime? = null,
    val deleted: Boolean = false
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      task: Task
  ) : this(
      task.identifier,
      task.version,
      task.createdBy.get(),
      task.createdDate.get(),
      task.lastModifiedBy.get(),
      task.lastModifiedDate.get(),
      task.project.identifier,
      task.name,
      task.description,
      task.location,
      task.projectCraft.identifier,
      task.assignee?.identifier,
      task.workArea?.identifier,
      task.status,
      task.topics?.map { it.identifier },
      task.editDate,
      task.deleted)
}

fun TaskSnapshot.toCommandHandler() = CommandHandler.of(this, TaskAvroSnapshotMapper)

fun Task.asSnapshot() = TaskSnapshot(this)
