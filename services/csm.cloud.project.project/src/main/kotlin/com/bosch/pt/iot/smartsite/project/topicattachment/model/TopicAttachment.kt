/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.TOPIC_ATTACHMENT
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.topic.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

@Entity
@DiscriminatorValue("2")
class TopicAttachment : Attachment<TopicAttachmentEventEnumAvro, TopicAttachment> {

  constructor()

  constructor(
      captureDate: Date?,
      fileName: String,
      fileSize: Long,
      imageHeight: Long,
      imageWidth: Long,
      task: Task,
      topic: Topic?,
  ) : super(captureDate, fileName, fileSize, imageHeight, imageWidth, task, topic)

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(false).buildAggregateIdentifier(),
          topic!!.task.project.identifier.toUuid())

  override fun toAvroMessage(): SpecificRecord =
      TopicAttachmentEventAvro.newBuilder()
          .setName(eventType)
          .setAggregateBuilder(
              TopicAttachmentAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifier(false))
                  .setAuditingInformation(toAuditingInformationAvro(false))
                  .setAttachment(toAttachmentAvro())
                  .setTopic(topic!!.identifier.toAggregateReference()))
          .build()

  override fun getChannel(): String = KafkaTopicProperties.PROJECT_BINDING

  override fun getOwnerType(): OwnerType = TOPIC_ATTACHMENT

  override fun getAggregateType(): String = ProjectmanagementAggregateTypeEnum.TOPICATTACHMENT.value

  companion object {
    private const val serialVersionUID: Long = 7214817314620271475
  }
}
