/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.TASK_ATTACHMENT
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

@Entity
@DiscriminatorValue("1")
class TaskAttachment : Attachment<TaskAttachmentEventEnumAvro, TaskAttachment> {

  constructor()

  constructor(
      captureDate: Date?,
      fileName: String,
      fileSize: Long,
      imageHeight: Long,
      imageWidth: Long,
      task: Task
  ) : super(captureDate, fileName, fileSize, imageHeight, imageWidth, task)

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(DELETED == eventType).buildAggregateIdentifier(),
          task!!.project.identifier.toUuid())

  override fun toAvroMessage(): SpecificRecord =
      TaskAttachmentEventAvro.newBuilder()
          .setName(eventType)
          .setAggregateBuilder(
              TaskAttachmentAggregateAvro.newBuilder()
                  .setAggregateIdentifier(this.toAggregateIdentifier(DELETED == eventType))
                  .setAuditingInformation(toAuditingInformationAvro(DELETED == eventType))
                  .setAttachment(toAttachmentAvro())
                  .setTask(task!!.identifier.toAggregateReference()))
          .build()

  override fun getChannel(): String = KafkaTopicProperties.PROJECT_BINDING

  override fun getOwnerType(): OwnerType = TASK_ATTACHMENT

  override fun getAggregateType(): String = TASKATTACHMENT.value

  companion object {
    private const val serialVersionUID: Long = -7724492136535729444
  }
}
