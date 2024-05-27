/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_CREATED_CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_CREATED_CRITICAL_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_CREATED_UNCRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_CREATED_UNCRITICAL_WITHOUT_TEXT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.UNCRITICAL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify topic created activity")
@SmartSiteSpringBootTest
class TopicCreatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }
  private val topic by lazy { context["topic"] as TopicAggregateG2Avro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
      it.status = DRAFT
      // setting all non-mandatory fields to null
      it.location = null
      it.description = null
    }
  }

  @Test
  fun `when the topic is critical and with description`() {
    eventStreamGenerator.submitTopicG2 {
      it.criticality = CRITICAL
      it.description = "description"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TOPIC_ACTIVITY_CREATED_CRITICAL, true)))
  }

  @Test
  fun `when the topic is critical without description`() {
    eventStreamGenerator.submitTopicG2 {
      it.criticality = CRITICAL
      it.description = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TOPIC_ACTIVITY_CREATED_CRITICAL_WITHOUT_TEXT, false)))
  }

  @Test
  fun `when the topic is uncritical and with description`() {
    eventStreamGenerator.submitTopicG2 {
      it.criticality = UNCRITICAL
      it.description = "description"
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TOPIC_ACTIVITY_CREATED_UNCRITICAL, true)))
  }

  @Test
  fun `when the topic is uncritical without description`() {
    eventStreamGenerator.submitTopicG2 {
      it.criticality = UNCRITICAL
      it.description = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TOPIC_ACTIVITY_CREATED_UNCRITICAL_WITHOUT_TEXT, false)))
  }

  private fun buildSummary(messageKey: String, topicDescriptionExists: Boolean): SummaryDto {
    val objectReferences =
        if (topicDescriptionExists) {
          mapOf(
              "originator" to
                  buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)),
              "topic" to buildPlaceholder(topic.getAggregateIdentifier(), topic.getDescription()))
        } else {
          mapOf(
              "originator" to
                  buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)))
        }

    return buildSummary(messageKey = messageKey, objectReferences = objectReferences)
  }
}
