/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.asTopicId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.extension.asCriticality
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TopicRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: TopicAggregateG2Avro

  lateinit var aggregateV1: TopicAggregateG2Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    submitBaseEvents()
  }

  @Test
  fun `query topic with all parameters set`() {
    submitEvents(true)

    // Execute query
    val topicList = query(false)

    assertThat(topicList.topics).hasSize(2)
    val topicV0 = topicList.topics[0]
    val topicV1 = topicList.topics[1]

    // Validate mandatory fields
    assertThat(topicV0.id).isEqualTo(aggregateV0.getIdentifier().asTopicId())
    assertThat(topicV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(topicV0.criticality).isEqualTo(aggregateV0.criticality.asCriticality().key)
    assertThat(topicV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(topicV0.deleted).isFalse
    // Check optional fields
    assertThat(topicV0.description).isEqualTo(aggregateV0.description)

    // Validate mandatory fields
    assertThat(topicV1.id).isEqualTo(aggregateV1.getIdentifier().asTopicId())
    assertThat(topicV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(topicV1.criticality).isEqualTo(aggregateV1.criticality.asCriticality().key)
    assertThat(topicV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(topicV1.deleted).isFalse
    // Check optional fields
    assertThat(topicV1.description).isEqualTo(aggregateV1.description)
  }

  @Test
  fun `query topic with all parameters set - latest only`() {
    submitEvents(true)

    // Execute query
    val topicList = query(true)

    assertThat(topicList.topics).hasSize(1)
    val topicV1 = topicList.topics.first()

    // Validate mandatory fields
    assertThat(topicV1.id).isEqualTo(aggregateV1.getIdentifier().asTopicId())
    assertThat(topicV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(topicV1.criticality).isEqualTo(aggregateV1.criticality.asCriticality().key)
    assertThat(topicV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(topicV1.deleted).isFalse
    // Validate optional fields
    assertThat(topicV1.description).isEqualTo(aggregateV1.description)
  }

  @Test
  fun `query topic without optional parameters set`() {
    submitEvents(false)

    // Execute query
    val topicList = query(false)

    assertThat(topicList.topics).hasSize(2)
    val topicV0 = topicList.topics[0]
    val topicV1 = topicList.topics[1]

    // Validate mandatory fields
    assertThat(topicV0.id).isEqualTo(aggregateV0.getIdentifier().asTopicId())
    assertThat(topicV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(topicV0.criticality).isEqualTo(aggregateV0.criticality.asCriticality().key)
    assertThat(topicV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(topicV0.deleted).isFalse
    // Validate optional fields
    assertThat(topicV0.description).isNull()

    // Validate mandatory fields
    assertThat(topicV1.id).isEqualTo(aggregateV1.getIdentifier().asTopicId())
    assertThat(topicV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(topicV1.criticality).isEqualTo(aggregateV1.criticality.asCriticality().key)
    assertThat(topicV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(topicV1.deleted).isFalse
    // Validate optional fields
    assertThat(topicV1.description).isNull()
  }

  @Test
  fun `query topic without optional parameters set - latest only`() {
    submitEvents(false)

    // Execute query
    val topicList = query(true)

    assertThat(topicList.topics).hasSize(1)
    val topicV1 = topicList.topics.first()

    // Validate mandatory fields
    assertThat(topicV1.id).isEqualTo(aggregateV1.getIdentifier().asTopicId())
    assertThat(topicV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(topicV1.criticality).isEqualTo(aggregateV1.criticality.asCriticality().key)
    assertThat(topicV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(topicV1.deleted).isFalse
    // Validate optional fields
    assertThat(topicV1.description).isNull()
  }

  @Test
  fun `query deleted topic`() {
    submitDeletedEvent()
    val topicList = query(false)

    assertThat(topicList.topics).hasSize(2)
    val topicV0 = topicList.topics[0]
    val topicV1 = topicList.topics[1]

    assertThat(topicV0.id).isEqualTo(aggregateV0.getIdentifier().asTopicId())
    assertThat(topicV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(topicV0.criticality).isEqualTo(aggregateV0.criticality.asCriticality().key)
    assertThat(topicV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(topicV0.deleted).isFalse
    assertThat(topicV0.description).isEqualTo(aggregateV0.description)

    assertThat(topicV1.id).isEqualTo(aggregateV1.getIdentifier().asTopicId())
    assertThat(topicV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(topicV1.criticality).isEqualTo(aggregateV1.criticality.asCriticality().key)
    assertThat(topicV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(topicV1.deleted).isTrue
    assertThat(topicV1.description).isEqualTo(aggregateV1.description)
  }

  @Test
  fun `query deleted topic latest only`() {
    submitDeletedEvent()
    val topicList = query(true)

    assertThat(topicList.topics).isEmpty()
  }

  private fun submitEvents(includeOptionals: Boolean) {

    aggregateV0 =
        eventStreamGenerator
            .submitTopicG2 {
              if (includeOptionals) {
                it.description = "Example description text"
              } else {
                it.description = null
              }
            }
            .get("topic")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTopicG2(eventType = TopicEventEnumAvro.UPDATED) {
              it.criticality = TopicCriticalityEnumAvro.CRITICAL
            }
            .get("topic")!!
  }

  private fun submitDeletedEvent() {
    aggregateV0 =
        eventStreamGenerator
            .submitTopicG2 { it.description = "Example description text" }
            .get("topic")!!

    aggregateV1 =
        eventStreamGenerator.submitTopicG2(eventType = TopicEventEnumAvro.DELETED).get("topic")!!
  }

  private fun submitBaseEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2().submitTask()
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/tasks/topics"),
          latestOnly,
          TopicListResource::class.java)
}
