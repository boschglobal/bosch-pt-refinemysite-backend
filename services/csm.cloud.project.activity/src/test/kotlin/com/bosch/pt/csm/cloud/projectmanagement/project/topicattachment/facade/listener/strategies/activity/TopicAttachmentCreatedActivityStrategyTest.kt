/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topicattachment.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify topic attachment created activity")
@SmartSiteSpringBootTest
class TopicAttachmentCreatedActivityStrategyTest : AbstractActivityIntegrationTest() {

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
  fun `when there is a topic description`() {
    eventStreamGenerator.submitTopicG2().submitTopicAttachment()

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(fmUser))
        .andExpect(
            ActivityMatchers.hasSummary(buildSummary(Key.TOPIC_ATTACHMENT_ACTIVITY_SAVED, true)))
        .andExpect(
            ActivityMatchers.hasChange(
                translate(Key.BARE_PARAMETERS_ONE_PARAMETER, "myPicture.jpg")))
  }

  @Test
  fun `when there is no topic description`() {
    eventStreamGenerator
        .submitTopicG2(asReference = "topicWithoutDescription") { it.description = null }
        .submitTopicAttachment { it.topic = getByReference("topicWithoutDescription") }

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(fmUser))
        .andExpect(
            ActivityMatchers.hasSummary(
                buildSummary(Key.TOPIC_ATTACHMENT_ACTIVITY_SAVED_TOPIC_WITHOUT_TEXT, false)))
        .andExpect(
            ActivityMatchers.hasChange(
                translate(Key.BARE_PARAMETERS_ONE_PARAMETER, "myPicture.jpg")))
  }

  private fun buildSummary(
      messageKey: String,
      topicDescriptionExists: Boolean,
      changesCount: Int = 1
  ): SummaryDto {
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

    return buildSummary(
        messageKey = messageKey,
        namedArguments = mapOf("count" to changesCount.toString()),
        objectReferences = objectReferences)
  }
}
