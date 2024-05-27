/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class CleanupStateFromMessageDeletedEventTest : AbstractIntegrationTest() {

  private val message by lazy { context["message"] as MessageAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .submitTopicG2 { it.description = "topic" }
        .setUserContext("csm-user")
        .submitMessage()
  }

  @Test
  fun `is cleaned up after message deleted event`() {
    assertThat(findMessage()).isNotNull
    eventStreamGenerator.submitMessage(eventType = DELETED)
    assertThat(findMessage()).isNull()
  }

  private fun findMessage() =
      repositories.messageRepository.find(
          identifier = message.getIdentifier(),
          version = message.getAggregateIdentifier().getVersion(),
          projectIdentifier = project.getIdentifier())
}
