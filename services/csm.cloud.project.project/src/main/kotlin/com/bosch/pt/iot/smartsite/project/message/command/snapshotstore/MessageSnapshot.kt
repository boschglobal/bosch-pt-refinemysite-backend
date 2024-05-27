/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.iot.smartsite.project.message.command.mapper.MessageAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.time.LocalDateTime

data class MessageSnapshot(
    override val identifier: MessageId,
    override val version: Long = VersionedSnapshot.INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val content: String?,
    val topicIdentifier: TopicId,
    val projectIdentifier: ProjectId,
) : VersionedSnapshot, AuditableSnapshot {
  constructor(
      message: Message
  ) : this(
      identifier = message.identifier,
      version = message.version,
      createdBy = message.createdBy.get(),
      createdDate = message.createdDate.get(),
      lastModifiedBy = message.lastModifiedBy.get(),
      lastModifiedDate = message.lastModifiedDate.get(),
      content = message.content,
      topicIdentifier = message.topic.identifier,
      projectIdentifier = message.topic.task.project.identifier,
  )
}

fun MessageSnapshot.toCommandHandler() = CommandHandler.of(this, MessageAvroSnapshotMapper)

fun Message.asSnapshot() = MessageSnapshot(this)
