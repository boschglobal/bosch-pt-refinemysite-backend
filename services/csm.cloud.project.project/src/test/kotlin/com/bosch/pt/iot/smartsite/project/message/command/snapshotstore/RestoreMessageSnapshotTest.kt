/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.snapshotstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
class RestoreMessageSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val topicIdentifier by lazy { getIdentifier("topic").asTopicId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  fun `validate that message created event was processed successfully`() {
    val message = repositories.findMessage(getIdentifier("message").asMessageId())!!
    val aggregate = get<MessageAggregateAvro>("message")!!

    validateMessage(message, aggregate, topicIdentifier)
  }

  @Test
  fun `validate message deleted event deletes a message`() {
    // Create a new message with multiple attachments and delete it testing idempotency
    eventStreamGenerator
        .submitMessage(asReference = "anotherMessage")
        .submitMessageAttachment(asReference = "anotherMessageAttachment1")
        .submitMessageAttachment(asReference = "anotherMessageAttachment2")
        .submitMessage(asReference = "anotherMessage", eventType = DELETED)
        .repeat(1)

    assertThat(repositories.findMessage(getIdentifier("anotherMessage").asMessageId())).isNull()
    assertThat(repositories.findMessageAttachment(getIdentifier("anotherMessageAttachment1")))
        .isNull()
    assertThat(repositories.findMessageAttachment(getIdentifier("anotherMessageAttachment2")))
        .isNull()
  }

  private fun validateMessage(
      message: Message,
      aggregate: MessageAggregateAvro,
      topicIdentifier: TopicId
  ) =
      with(message) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(topic.identifier).isEqualTo(topicIdentifier)
        assertThat(message.content).isEqualTo(aggregate.content)
      }
}
