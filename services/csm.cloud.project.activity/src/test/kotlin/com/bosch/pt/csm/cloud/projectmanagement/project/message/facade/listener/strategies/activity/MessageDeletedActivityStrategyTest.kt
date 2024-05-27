/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasNoChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify message deleted activity")
@SmartSiteSpringBootTest
class MessageDeletedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }
  private val topic by lazy { context["topic"] as TopicAggregateG2Avro }
  private val message by lazy { context["message"] as MessageAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
          it.status = DRAFT
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitTopicG2 { it.description = "topic" }
        .submitTopicG2(asReference = "topicWithoutDescription") { it.description = null }
        .submitMessage()
  }

  @Test
  fun `when there is a topic description and content`() {
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED) {
      it.topic = getByReference("topic")
      it.content = "comment"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(
            hasSummary(
                buildSummary(
                    Key.MESSAGE_ACTIVITY_DELETED,
                    topicDescriptionExists = true,
                    contentExists = true)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when there is a topic description but no content`() {
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED) {
      it.topic = getByReference("topic")
      it.content = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(
            hasSummary(
                buildSummary(
                    Key.MESSAGE_ACTIVITY_DELETED_MESSAGE_WITHOUT_TEXT,
                    topicDescriptionExists = true,
                    contentExists = false)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when there is no topic description but content`() {
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED) {
      it.content = "comment"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(
            hasSummary(
                buildSummary(
                    Key.MESSAGE_ACTIVITY_DELETED,
                    topicDescriptionExists = false,
                    contentExists = true)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when there is no topic description and no content`() {
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED) {
      it.content = null
    }

    requestActivities(task)
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(
            hasSummary(
                buildSummary(
                    Key.MESSAGE_ACTIVITY_DELETED_MESSAGE_AND_TOPIC_WITHOUT_TEXT,
                    topicDescriptionExists = false,
                    contentExists = false)))
        .andExpect(hasNoChanges())
  }

  private fun buildSummary(
      messageKey: String,
      topicDescriptionExists: Boolean,
      contentExists: Boolean
  ): SummaryDto {
    val objectReferences =
        when {
          !contentExists && !topicDescriptionExists ->
              mapOf(
                  "originator" to
                      buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)))
          !contentExists && topicDescriptionExists ->
              mapOf(
                  "originator" to
                      buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)),
                  "topic" to
                      buildPlaceholder(topic.getAggregateIdentifier(), topic.getDescription()))
          else ->
              mapOf(
                  "originator" to
                      buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)),
                  "comment" to
                      buildPlaceholder(message.getAggregateIdentifier(), message.getContent()))
        }

    return buildSummary(messageKey = messageKey, objectReferences = objectReferences)
  }
}
