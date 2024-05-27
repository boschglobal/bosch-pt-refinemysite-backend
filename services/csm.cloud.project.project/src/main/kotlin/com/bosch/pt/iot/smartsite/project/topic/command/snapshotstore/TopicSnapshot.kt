/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.command.mapper.TopicAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import java.time.LocalDateTime

data class TopicSnapshot(
    override val identifier: TopicId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val description: String? = null,
    val criticality: TopicCriticalityEnum,
    val taskIdentifier: TaskId,
    val messages: Set<MessageId> = emptySet(),
    val projectIdentifier: ProjectId,
    val deleted: Boolean = false
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      topic: Topic
  ) : this(
      identifier = topic.identifier,
      version = topic.version,
      createdBy = topic.createdBy.get(),
      createdDate = topic.createdDate.get(),
      lastModifiedBy = topic.lastModifiedBy.get(),
      lastModifiedDate = topic.lastModifiedDate.get(),
      description = topic.description,
      criticality = topic.criticality,
      taskIdentifier = topic.task.identifier,
      messages = topic.getMessages().map { it.identifier }.toSet(),
      projectIdentifier = topic.task.project.identifier,
      deleted = topic.deleted)
}

fun TopicSnapshot.toCommandHandler() = CommandHandler.of(this, TopicAvroSnapshotMapper)

fun Topic.asSnapshot() = TopicSnapshot(this)
