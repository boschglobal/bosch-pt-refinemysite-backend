/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TOPIC_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.request.CreateTopicResource
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicResource.Companion.EMBEDDED_TOPIC_ATTACHMENTS
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.TopicAttachmentListResource
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
class TopicIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TopicController

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }
  private val topic2 by lazy { repositories.findTopic(getIdentifier("topic2").asTopicId())!! }
  private val topic3 by lazy { repositories.findTopic(getIdentifier("topic3").asTopicId())!! }
  private val topic4 by lazy { repositories.findTopic(getIdentifier("topic4").asTopicId())!! }
  private val topic5 by lazy { repositories.findTopic(getIdentifier("topic5").asTopicId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTopicG2(asReference = "topic", eventType = TopicEventEnumAvro.UPDATED) {
          it.description = "topic description"
        }
        .submitTopicAttachment(asReference = "attachment1")
        .submitTopicAttachment(asReference = "attachment2")
        .submitMessage(asReference = "message")
        .submitTopicG2(asReference = "topic2") {
          it.description = "topic2 description"
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
        }
        .submitTopicG2(asReference = "topic3") {
          it.description = "topic3 description"
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
        .submitTopicG2(asReference = "topic4") {
          it.description = "topic4 description"
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
        }
        .submitTopicG2(asReference = "topic5") {
          it.description = "topic5 description"
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
        .submitTask(asReference = "otherTask")
        .submitTopicG2(asReference = "criticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
        }
        .submitTopicAttachment(asReference = "otherAttachment")
        .submitMessage(asReference = "otherMessage1")
        .submitTopicG2(asReference = "uncriticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
        .submitTopicAttachment(asReference = "otherAttachment2")
        .submitMessage(asReference = "otherMessage2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create topic with no description`() {
    val topicResource =
        cut.createTopic(task.identifier, null, CreateTopicResource(null, UNCRITICAL)).body!!

    assertThat(topicResource.description).isNull()
  }

  @Test
  fun `verify find one topic with the corresponding attachments`() {
    val topicResource = cut.findTopic(topic.identifier).body!!
    val attachments =
        topicResource.embeddedResources[EMBEDDED_TOPIC_ATTACHMENTS] as TopicAttachmentListResource

    assertThat(topicResource.id.asTopicId()).isEqualTo(topic.identifier)
    assertThat(topicResource.description).isEqualTo(topic.description)
    assertThat(topicResource.messages).isEqualTo(1L)
    assertThat(topicResource.criticality).isEqualTo(UNCRITICAL)
    assertThat(topicResource.taskId).isEqualTo(task.identifier)

    assertThat(attachments).isNotNull
    assertThat(attachments.attachments).isNotNull
    assertThat(attachments.attachments)
        .extracting<UUID> { it.identifier }
        .containsExactly(getIdentifier("attachment2"), getIdentifier("attachment1"))
  }

  @Test
  fun `verify find all topics by task using the before and limit`() {
    val topicListResource = cut.findAllTopics(task.identifier, topic4.identifier, 2).body!!

    assertThat(topicListResource.topics)
        .hasSize(2)
        .extracting("id", "description", "criticality", "messages", "taskId")
        .containsExactlyInAnyOrder(
            tuple(
                topic2.identifier.toUuid(),
                topic2.description,
                topic2.criticality,
                0L,
                task.identifier),
            tuple(
                topic3.identifier.toUuid(),
                topic3.description,
                topic3.criticality,
                0L,
                task.identifier))
  }

  @Test
  fun `verify find all topics by task only using the before`() {
    val topicListResource = cut.findAllTopics(task.identifier, topic4.identifier, null).body!!

    assertThat(topicListResource.topics)
        .hasSize(3)
        .extracting("id", "description", "criticality", "messages", "taskId")
        .containsExactlyInAnyOrder(
            tuple(
                topic3.identifier.toUuid(),
                topic3.description,
                topic3.criticality,
                0L,
                task.identifier),
            tuple(
                topic2.identifier.toUuid(),
                topic2.description,
                topic2.criticality,
                0L,
                task.identifier),
            tuple(
                topic.identifier.toUuid(),
                topic.description,
                topic.criticality,
                1L,
                task.identifier))
  }

  @Test
  fun `verify find all topics by task not using the before and limit`() {
    val topicListResource = cut.findAllTopics(task.identifier, null, null).body!!

    assertThat(topicListResource.topics)
        .hasSize(5)
        .extracting("id", "description", "criticality", "messages", "taskId")
        .containsExactlyInAnyOrder(
            tuple(
                topic.identifier.toUuid(),
                topic.description,
                topic.criticality,
                1L,
                task.identifier),
            tuple(
                topic2.identifier.toUuid(),
                topic2.description,
                topic2.criticality,
                0L,
                task.identifier),
            tuple(
                topic3.identifier.toUuid(),
                topic3.description,
                topic3.criticality,
                0L,
                task.identifier),
            tuple(
                topic4.identifier.toUuid(),
                topic4.description,
                topic4.criticality,
                0L,
                task.identifier),
            tuple(
                topic5.identifier.toUuid(),
                topic5.description,
                topic5.criticality,
                0L,
                task.identifier))
  }

  @Test
  fun `verify escalate topic that is already critical`() {
    cut.escalateTopic(getIdentifier("criticalTopic").asTopicId())

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify deescalate topic that is already uncritical`() {
    cut.deEscalateTopic(getIdentifier("uncriticalTopic").asTopicId())

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create topic for non-existing task fails`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.createTopic(TaskId(), TopicId(), CreateTopicResource("description", UNCRITICAL))
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find topic with non-existing identifier fails`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.findTopic(TopicId())
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all topics by non-existing task fails`() {

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.findAllTopics(TaskId(), null, null)
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all topics by non-existing before topic fails`() {
    val identifier = TopicId()

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findAllTopics(task.identifier, identifier, null) }
        .withMessageKey(TOPIC_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all topics in batch by non-existing task fails`() {
    assertThatThrownBy {
          cut.findByTaskIdentifiers(
              BatchRequestResource(setOf(randomUUID())), PageRequest.of(0, 5), TASK)
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find all topics in batch by task for non supported identifier type fails`() {
    assertThatThrownBy {
          cut.findByTaskIdentifiers(
              BatchRequestResource(setOf(task.identifier.toUuid())), PageRequest.of(0, 5), DAYCARD)
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            BatchIdentifierTypeNotSupportedException(
                COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify escalate topic with non-existing identifier fails`() {
    assertThatThrownBy { cut.escalateTopic(TopicId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify deescalate topic with non-existing identifier fails`() {
    assertThatThrownBy { cut.deEscalateTopic(TopicId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete topic with non-existing identifier fails`() {
    assertThatThrownBy { cut.deleteTopic(TopicId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }
}
