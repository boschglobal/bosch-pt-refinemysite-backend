/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.util.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RestoreTaskAttachmentStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitTaskAttachment()
  }

  @Test
  fun `validate that task attachment created event was processed successfully`() {
    val taskAttachmentAggregate = get<TaskAttachmentAggregateAvro>("taskAttachment")!!

    val taskAttachment =
        repositories.taskAttachmentRepository.findOneWithDetailsByIdentifier(
            taskAttachmentAggregate.getIdentifier())!!

    validateBasicAttributes(taskAttachment, taskAttachmentAggregate)
    validateAuditableAndVersionedEntityAttributes(taskAttachment, taskAttachmentAggregate)
  }

  @Test
  fun `validate that task attachment updated event was processed successfully`() {
    eventStreamGenerator.submitTaskAttachment(eventType = UPDATED) {
      it.attachmentBuilder.smallAvailable = true
    }

    val taskAttachmentAggregate = get<TaskAttachmentAggregateAvro>("taskAttachment")!!

    val taskAttachment =
        repositories.taskAttachmentRepository.findOneWithDetailsByIdentifier(
            taskAttachmentAggregate.getIdentifier())!!

    assertThat(taskAttachment.isSmallAvailable()).isTrue
    validateBasicAttributes(taskAttachment, taskAttachmentAggregate)
    validateAuditableAndVersionedEntityAttributes(taskAttachment, taskAttachmentAggregate)
  }

  @Test
  fun `validate task attachment deleted event deletes a task attachment`() {
    assertThat(repositories.taskAttachmentRepository.findAll()).hasSize(1)

    eventStreamGenerator.submitTaskAttachment(eventType = DELETED)

    assertThat(repositories.taskAttachmentRepository.findAll()).isEmpty()

    // Send event again to test idempotency
    assertDoesNotThrow { eventStreamGenerator.repeat(1) }
  }

  private fun validateBasicAttributes(
      taskAttachment: TaskAttachment,
      taskAttachmentAggregate: TaskAttachmentAggregateAvro
  ) {
    val attachmentAvro = taskAttachmentAggregate.attachment

    assertThat(taskAttachment.identifier).isEqualTo(taskAttachmentAggregate.getIdentifier())
    assertThat(taskAttachment.version)
        .isEqualTo(taskAttachmentAggregate.aggregateIdentifier.version)
    assertThat(taskAttachment.task!!.identifier)
        .isEqualTo(taskAttachmentAggregate.task.identifier.asTaskId())
    assertThat(taskAttachment.isSmallAvailable()).isEqualTo(attachmentAvro.smallAvailable)
    assertThat(taskAttachment.fileName).isEqualTo(attachmentAvro.fileName)
    assertThat(taskAttachment.fileSize).isEqualTo(attachmentAvro.fileSize)
    assertThat(taskAttachment.imageHeight).isEqualTo(attachmentAvro.height)
    assertThat(taskAttachment.imageWidth).isEqualTo(attachmentAvro.width)
    assertThat(taskAttachment.isFullAvailable()).isEqualTo(attachmentAvro.fullAvailable)

    if (taskAttachment.captureDate?.time != null) {
      assertThat(taskAttachmentAggregate.attachment.captureDate)
          .isEqualTo(taskAttachment.captureDate?.time)
    }
  }
}
