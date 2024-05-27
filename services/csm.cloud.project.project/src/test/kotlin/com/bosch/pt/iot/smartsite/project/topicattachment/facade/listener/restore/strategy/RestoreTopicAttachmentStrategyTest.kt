/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.sql.Timestamp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreTopicAttachmentStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitTopicAttachment()
  }

  @Test
  fun `validate that topic attachment created event was processed successfully`() {
    val topicAttachmentAggregate = get<TopicAttachmentAggregateAvro>("topicAttachment")!!
    val taskAggregate = get<TaskAggregateAvro>("task")!!

    val topicAttachment =
        repositories.topicAttachmentRepository.findOneWithDetailsByIdentifier(
            topicAttachmentAggregate.getIdentifier())!!

    validateBasicAttributes(topicAttachment, topicAttachmentAggregate, taskAggregate)
    validateAuditableAndVersionedEntityAttributes(topicAttachment, topicAttachmentAggregate)
  }

  @Test
  fun `validate that topic attachment updated event was processed successfully`() {
    eventStreamGenerator.submitTopicAttachment(eventType = UPDATED) {
      it.attachmentBuilder.smallAvailable = true
    }

    val topicAttachmentAggregate = get<TopicAttachmentAggregateAvro>("topicAttachment")!!
    val taskAggregate = get<TaskAggregateAvro>("task")!!

    val topicAttachment =
        repositories.topicAttachmentRepository.findOneWithDetailsByIdentifier(
            topicAttachmentAggregate.getIdentifier())!!

    assertThat(topicAttachment.isSmallAvailable()).isTrue
    validateBasicAttributes(topicAttachment, topicAttachmentAggregate, taskAggregate)
    validateAuditableAndVersionedEntityAttributes(topicAttachment, topicAttachmentAggregate)
  }

  private fun validateBasicAttributes(
      topicAttachment: TopicAttachment,
      topicAttachmentAggregate: TopicAttachmentAggregateAvro,
      taskAggregate: TaskAggregateAvro
  ) {
    val attachmentAvro = topicAttachmentAggregate.attachment

    assertThat(topicAttachment.topic!!.identifier)
        .isEqualTo(topicAttachmentAggregate.topic.identifier.asTopicId())
    assertThat(topicAttachment.task!!.identifier)
        .isEqualTo(taskAggregate.getIdentifier().asTaskId())
    assertThat(topicAttachment.isSmallAvailable()).isEqualTo(attachmentAvro.smallAvailable)
    assertThat(topicAttachment.fileName).isEqualTo(attachmentAvro.fileName)
    assertThat(topicAttachment.fileSize).isEqualTo(attachmentAvro.fileSize)
    assertThat(topicAttachment.imageHeight).isEqualTo(attachmentAvro.height)
    assertThat(topicAttachment.imageWidth).isEqualTo(attachmentAvro.width)
    assertThat(topicAttachment.isFullAvailable()).isEqualTo(attachmentAvro.fullAvailable)

    if (attachmentAvro.captureDate != null) {
      assertThat(topicAttachment.captureDate).isEqualTo(Timestamp(attachmentAvro.captureDate))
    }
  }
}
