/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.sql.Timestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
class RestoreMessageAttachmentStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitMessageAttachment()
  }

  @Test
  fun `validate that message attachment created event was processed successfully`() {
    val messageAttachmentAggregate = get<MessageAttachmentAggregateAvro>("messageAttachment")!!
    val topicAggregate = get<TopicAggregateG2Avro>("topic")!!
    val taskAggregate = get<TaskAggregateAvro>("task")!!

    val messageAttachment =
        repositories.messageAttachmentRepository.findOneWithDetailsByIdentifier(
            messageAttachmentAggregate.getIdentifier())!!

    validateBasicAttributes(
        messageAttachment, messageAttachmentAggregate, topicAggregate, taskAggregate)
    validateAuditableAndVersionedEntityAttributes(messageAttachment, messageAttachmentAggregate)
  }

  @Test
  fun `validate that message attachment updated event was processed successfully`() {
    eventStreamGenerator.submitMessageAttachment(eventType = UPDATED) {
      it.attachmentBuilder.smallAvailable = true
    }

    val messageAttachmentAggregate = get<MessageAttachmentAggregateAvro>("messageAttachment")!!
    val topicAggregate = get<TopicAggregateG2Avro>("topic")!!
    val taskAggregate = get<TaskAggregateAvro>("task")!!

    val messageAttachment =
        repositories.messageAttachmentRepository.findOneWithDetailsByIdentifier(
            messageAttachmentAggregate.getIdentifier())!!

    assertThat(messageAttachment.isSmallAvailable()).isTrue()
    validateBasicAttributes(
        messageAttachment, messageAttachmentAggregate, topicAggregate, taskAggregate)
    validateAuditableAndVersionedEntityAttributes(messageAttachment, messageAttachmentAggregate)
  }

  private fun validateBasicAttributes(
      messageAttachment: MessageAttachment,
      messageAttachmentAggregate: MessageAttachmentAggregateAvro,
      topicAggregate: TopicAggregateG2Avro,
      taskAggregate: TaskAggregateAvro
  ) {
    val attachmentAvro = messageAttachmentAggregate.attachment

    assertThat(messageAttachment.identifier).isEqualTo(messageAttachmentAggregate.getIdentifier())
    assertThat(messageAttachment.version)
        .isEqualTo(messageAttachmentAggregate.aggregateIdentifier.version)
    assertThat(messageAttachment.message!!.identifier)
        .isEqualTo(messageAttachmentAggregate.message.identifier.asMessageId())
    assertThat(messageAttachment.topic!!.identifier)
        .isEqualTo(topicAggregate.getIdentifier().asTopicId())
    assertThat(messageAttachment.task!!.identifier)
        .isEqualTo(taskAggregate.getIdentifier().asTaskId())
    assertThat(messageAttachment.isSmallAvailable()).isEqualTo(attachmentAvro.smallAvailable)
    assertThat(messageAttachment.fileName).isEqualTo(attachmentAvro.fileName)
    assertThat(messageAttachment.fileSize).isEqualTo(attachmentAvro.fileSize)
    assertThat(messageAttachment.imageHeight).isEqualTo(attachmentAvro.height)
    assertThat(messageAttachment.imageWidth).isEqualTo(attachmentAvro.width)
    assertThat(messageAttachment.isFullAvailable()).isEqualTo(attachmentAvro.fullAvailable)

    if (attachmentAvro.captureDate != null) {
      assertThat(messageAttachment.captureDate).isEqualTo(Timestamp(attachmentAvro.captureDate))
    }
  }
}
