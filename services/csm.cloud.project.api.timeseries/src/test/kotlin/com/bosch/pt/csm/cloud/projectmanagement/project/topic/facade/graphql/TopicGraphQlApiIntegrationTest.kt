/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.extension.asCriticality
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.isNull
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TopicGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  lateinit var aggregateV0: TopicAggregateG2Avro

  lateinit var aggregateV1: TopicAggregateG2Avro

  val topic = "projects[0].tasks[0].topics[0]"

  val query =
      """
      query {
        projects {
          tasks {
            topics {
              id
              version
              criticality
              description
              eventDate
            }
          }
        }
      }
      """.trimIndent()

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query topic with all parameters set`() {
    submitEvents(true)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$topic.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$topic.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())

    response.get("$topic.criticality").isEqualTo(aggregateV1.criticality.asCriticality().shortKey)
    response.get("$topic.description").isEqualTo(aggregateV1.description)
    response.get("$topic.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query topic with optional parameters not set`() {
    submitEvents(false)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.get("$topic.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$topic.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())

    response.get("$topic.criticality").isEqualTo(aggregateV1.criticality.asCriticality().shortKey)
    response.isNull("$topic.description")
    response.get("$topic.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query deleted topic`() {
    submitEvents(true)
    eventStreamGenerator.submitTopicG2("topic", eventType = TopicEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.isNull(topic)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskSchedule()

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
}
