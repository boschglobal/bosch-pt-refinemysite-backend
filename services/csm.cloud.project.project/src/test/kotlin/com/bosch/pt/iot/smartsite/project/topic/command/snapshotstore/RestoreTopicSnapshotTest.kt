/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.UNCRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
class RestoreTopicSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  fun `validate that topic created event was processed successfully`() {
    val topic = repositories.findTopic(getIdentifier("topic").asTopicId())!!
    val aggregate = get<TopicAggregateG2Avro>("topic")!!

    validateTopic(topic, aggregate, taskIdentifier)
  }

  // At the moment there is no update functionality for the topic however because the
  // TopicEventEnumAvro.UPDATED exists there should be a restore test for it
  @Test
  fun `validate that topic updated event was processed successfully`() {
    eventStreamGenerator.submitTopicG2(asReference = "otherTopic").submitTopicG2(
        asReference = "otherTopic", eventType = UPDATED) {
          it.description = "Updated topic description"
        }

    val topic = repositories.findTopic(getIdentifier("otherTopic").asTopicId())!!
    val aggregate = get<TopicAggregateG2Avro>("otherTopic")!!

    validateTopic(topic, aggregate, taskIdentifier)
  }

  @Test
  fun `validate that topic escalated event was processed successfully`() {
    eventStreamGenerator
        .submitTopicG2(asReference = "anotherTopic") {
          it.description = "Topic description"
          it.criticality = UNCRITICAL
        }
        .submitTopicG2(asReference = "anotherTopic", eventType = ESCALATED) {
          it.criticality = CRITICAL
        }

    val topic = repositories.findTopic(getIdentifier("anotherTopic").asTopicId())!!
    val aggregate = get<TopicAggregateG2Avro>("anotherTopic")!!

    validateTopic(topic, aggregate, taskIdentifier)
  }

  @Test
  fun `validate that topic deescalated event was processed successfully`() {
    eventStreamGenerator
        .submitTopicG2(asReference = "anotherTopic") {
          it.description = "Topic description"
          it.criticality = CRITICAL
        }
        .submitTopicG2(asReference = "anotherTopic", eventType = DEESCALATED) {
          it.criticality = UNCRITICAL
        }

    val topic = repositories.findTopic(getIdentifier("anotherTopic").asTopicId())!!
    val aggregate = get<TopicAggregateG2Avro>("anotherTopic")!!

    validateTopic(topic, aggregate, taskIdentifier)
  }

  @Test
  fun `validate topic deleted event deletes a topic`() {
    // Create a new topic with a full tree of message and attachments
    // and delete it testing idempotency
    eventStreamGenerator
        .submitTopicG2(asReference = "anotherTopic")
        .submitTopicAttachment(asReference = "anotherTopicAttachment")
        .submitMessage(asReference = "anotherMessage")
        .submitMessageAttachment(asReference = "anotherMessageAttachment")
        .submitTopicG2(asReference = "anotherTopic", eventType = DELETED)
        .repeat(1)

    assertThat(repositories.findTopic(getIdentifier("anotherTopic").asTopicId())).isNull()
    assertThat(repositories.findTopicAttachment(getIdentifier("anotherTopicAttachment"))).isNull()
    assertThat(repositories.findMessage(getIdentifier("anotherMessage").asMessageId())).isNull()
    assertThat(repositories.findMessageAttachment(getIdentifier("anotherMessageAttachment")))
        .isNull()
  }

  private fun validateTopic(topic: Topic, aggregate: TopicAggregateG2Avro, taskIdentifier: TaskId) =
      with(topic) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(task.identifier).isEqualTo(taskIdentifier)
        assertThat(criticality.name).isEqualTo(aggregate.criticality.name)
        assertThat(description).isEqualTo(aggregate.description)
      }
}
