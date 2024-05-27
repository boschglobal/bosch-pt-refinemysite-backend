/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MESSAGE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.request.CreateMessageResource
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageResource.Companion.EMBEDDED_MESSAGE_ATTACHMENTS
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentListResource
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class MessageIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: MessageController

  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }
  private val message by lazy { repositories.findMessage(getIdentifier("message").asMessageId())!! }
  private val message2 by lazy {
    repositories.findMessage(getIdentifier("message2").asMessageId())!!
  }
  private val message3 by lazy {
    repositories.findMessage(getIdentifier("message3").asMessageId())!!
  }
  private val message4 by lazy {
    repositories.findMessage(getIdentifier("message4").asMessageId())!!
  }
  private val message5 by lazy {
    repositories.findMessage(getIdentifier("message5").asMessageId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitMessage(asReference = "message")
        .submitMessageAttachment(asReference = "attachment1")
        .submitMessageAttachment(asReference = "attachment2")
        .submitMessage(asReference = "message2")
        .submitMessage(asReference = "message3")
        .submitMessage(asReference = "message4")
        .submitMessage(asReference = "message5")
        .submitTask(asReference = "otherTask")
        .submitTopicG2(asReference = "otherTopic")
        .submitMessage(asReference = "otherMessage")
        .submitMessageAttachment(asReference = "otherAttachment")
        .submitMessage(asReference = "otherMessage2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create message with no content`() {
    val messageIdentifier = MessageId()
    val messageResource =
        cut.createMessage(
                topic.identifier, messageIdentifier, CreateMessageResource("Test content"))
            .body!!

    assertThat(messageResource.id.asMessageId()).isEqualTo(messageIdentifier)
    assertThat(messageResource.topicId).isEqualTo(topic.identifier)
    assertThat(messageResource.content).isEqualTo("Test content")
  }

  @Test
  fun `verify find one message contains the corresponding attachments`() {
    val messageResource = cut.findMessage(message.identifier).body!!
    assertThat(messageResource.id.asMessageId()).isEqualTo(message.identifier)
    assertThat(messageResource.topicId).isEqualTo(topic.identifier)
    assertThat(messageResource.content).isEqualTo(message.content)

    val attachments =
        messageResource.embeddedResources[EMBEDDED_MESSAGE_ATTACHMENTS]
            as MessageAttachmentListResource

    assertThat(attachments).isNotNull
    assertThat(attachments.attachments).isNotNull
    assertThat(attachments.attachments)
        .extracting<UUID> { it.identifier }
        .containsExactly(getIdentifier("attachment2"), getIdentifier("attachment1"))
  }

  @Test
  fun `verify find all messages by topic using the before and limit`() {
    val messageListResource = cut.findAllMessages(topic.identifier, message4.identifier, 2).body!!

    assertThat(messageListResource.messages)
        .hasSize(2)
        .extracting("id", "content")
        .containsOnly(
            tuple(message3.identifier.identifier, message3.content),
            tuple(message2.identifier.identifier, message2.content))
  }

  @Test
  fun `verify find all messages by topic only using the before`() {
    val messageListResource =
        cut.findAllMessages(topic.identifier, message4.identifier, null).body!!

    assertThat(messageListResource.messages)
        .hasSize(3)
        .extracting("id", "content")
        .containsOnly(
            tuple(message3.identifier.identifier, message3.content),
            tuple(message2.identifier.identifier, message2.content),
            tuple(message.identifier.identifier, message.content))
  }

  @Test
  fun `verify find all messages by topic not using the before and limit`() {
    val messageListResource = cut.findAllMessages(topic.identifier, null, null).body!!

    assertThat(messageListResource.messages)
        .hasSize(5)
        .extracting("id", "content")
        .containsOnly(
            tuple(message5.identifier.identifier, message5.content),
            tuple(message4.identifier.identifier, message4.content),
            tuple(message3.identifier.identifier, message3.content),
            tuple(message2.identifier.identifier, message2.content),
            tuple(message.identifier.identifier, message.content))
  }

  @Test
  fun `verify create message for non-existing topic fails`() {
    assertThatThrownBy {
          cut.createMessage(TopicId(), MessageId(), CreateMessageResource("content"))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find message with non-existing identifier fails`() {
    assertThatThrownBy { cut.findMessage(MessageId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all messages by non-existing topic fails`() {
    assertThatThrownBy { cut.findAllMessages(TopicId(), null, null) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all messages by non-existing before message fails`() {
    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findAllMessages(topic.identifier, MessageId(), null) }
        .withMessageKey(MESSAGE_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all messages in batch by non-existing task fails`() {
    assertThatThrownBy {
          cut.findByTaskIdentifiers(
              BatchRequestResource(setOf(randomUUID())), PageRequest.of(0, 5), TASK)
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all messages in batch by task for non supported identifier type fails`() {
    assertThatThrownBy {
          cut.findByTaskIdentifiers(
              BatchRequestResource(setOf(getIdentifier("task"))), PageRequest.of(0, 5), DAYCARD)
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            BatchIdentifierTypeNotSupportedException(
                COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete message with non-existing identifier fails`() {
    assertThatThrownBy { cut.deleteMessage(MessageId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }
}
