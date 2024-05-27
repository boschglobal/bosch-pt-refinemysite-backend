/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromTopicEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.topicRepository.deleteAll()
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `is saved after topic created event`() {
    assertThat(repositories.topicRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTopicG2 { it.description = "description" }.submitTopicG2(
              asReference = "topicWithoutDescription") { it.description = null }
    }

    assertThat(repositories.topicRepository.findAll()).hasSize(2)
  }

  @Test
  fun `is updated after topic escalated event`() {
    assertThat(repositories.topicRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTopicG2 { it.description = "description" }
          .submitTopicG2(eventType = ESCALATED)
    }

    assertThat(repositories.topicRepository.findAll()).hasSize(2)
  }

  @Test
  fun `is updated and cleaned up after topic deescalated event`() {
    assertThat(repositories.topicRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTopicG2 { it.description = "description" }
          .submitTopicG2(eventType = ESCALATED)
      eventStreamGenerator.submitTopicG2(eventType = DEESCALATED)
    }

    val topics = repositories.topicRepository.findAll()
    assertThat(topics).hasSize(2)
    assertThat(topics)
        .extracting("identifier")
        .extracting("identifier")
        .containsOnly(getIdentifier("topic"))
    assertThat(topics).extracting("identifier").extracting("version").containsAll(listOf(1L, 2L))
  }
}
