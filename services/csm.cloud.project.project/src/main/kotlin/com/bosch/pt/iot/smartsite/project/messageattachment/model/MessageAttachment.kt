/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.MESSAGE_ATTACHMENT
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGEATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.message.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.util.Date
import java.util.UUID

@Entity
@DiscriminatorValue("3")
class MessageAttachment : Attachment<MessageAttachmentEventEnumAvro, MessageAttachment> {
  constructor()

  constructor(
      captureDate: Date?,
      fileName: String,
      fileSize: Long,
      imageHeight: Long,
      imageWidth: Long,
      task: Task,
      topic: Topic,
      message: Message,
  ) : super(captureDate, fileName, fileSize, imageHeight, imageWidth, task, topic, message)

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(false).buildAggregateIdentifier(),
          message!!.topic.task.project.identifier.toUuid())

  override fun toAvroMessage(): MessageAttachmentEventAvro =
      MessageAttachmentEventAvro.newBuilder()
          .setName(eventType)
          .setAggregateBuilder(
              MessageAttachmentAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifier(false))
                  .setAuditingInformation(toAuditingInformationAvro(false))
                  .setAttachment(toAttachmentAvro())
                  .setMessage(message!!.identifier.toAggregateReference()))
          .build()

  override fun getChannel() = PROJECT_BINDING

  override fun getOwnerType() = MESSAGE_ATTACHMENT

  override fun getAggregateType() = MESSAGEATTACHMENT.value

  companion object {
    private const val serialVersionUID: Long = -3367411594023868093
  }
}
