/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasActivitiesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasMessage
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasValidPrevLink
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectNotFound
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.ACTIVITY_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.DAY_CARD_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TOPIC_ACTIVITY_CREATED_UNCRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro.UNCRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@SmartSiteSpringBootTest
@DisplayName("Verify activity api")
class ActivityIntegrationTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }
  private val topic by lazy { context["topic"] as TopicAggregateG2Avro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
          it.status = OPEN
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Notes"
          it.reason = null
        }
        .submitTopicG2 {
          it.criticality = UNCRITICAL
          it.description = "Topic Description"
        }
  }

  @Test
  fun `fetch activities only from the given task in the correct order`() {

    eventStreamGenerator
        .setUserContext("cr-user")
        .submitTask(asReference = "otherTask") {
          it.assignee = getByReference("cr-participant")
          it.name = "otherTask"
          it.status = OPEN
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitDayCardG2(asReference = "otherDayCard") {
          it.task = getByReference("otherTask")
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.notes = "Daycard Notes"
          it.reason = null
        }
        .submitTopicG2(asReference = "otherTopic") {
          it.task = getByReference("otherTask")
          it.description = "Topic Description"
          it.criticality = UNCRITICAL
        }

    requestActivities(task = task, limit = 50)
        .andExpectOk()
        .andExpect(hasActivitiesCount(3))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTopic()))
        .andExpect(hasSummary(index = 1, summary = buildSummaryForDayCard()))
        .andExpect(hasSummary(index = 2, summary = buildSummaryForTask()))
  }

  @Test
  fun `fetch the only the number of activities from the given request`() {

    requestActivities(task = task, limit = 2)
        .andExpectOk()
        .andExpect(hasActivitiesCount(2))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTopic()))
        .andExpect(hasSummary(index = 1, summary = buildSummaryForDayCard()))
  }

  @Test
  fun `fetch without limit and before works properly`() {

    requestActivitiesWithoutLimitAndBefore(task = task)
        .andExpectOk()
        .andExpect(hasActivitiesCount(3))
        .andExpect(hasSummary(index = 0, summary = buildSummaryForTopic()))
        .andExpect(hasSummary(index = 1, summary = buildSummaryForDayCard()))
        .andExpect(hasSummary(index = 2, summary = buildSummaryForTask()))
  }

  @Test
  fun `sets the correct link for the previous`() {

    val activities =
        repositories.activityRepository.findAllByContextTask(
            task.getIdentifier(), PageRequest.of(0, 1, Sort.by(Sort.Order.desc("event.date"))))

    val firstActivityIdentifier = activities.first().identifier

    requestActivities(task = task, limit = 1)
        .andExpectOk()
        .andExpect(hasValidPrevLink(firstActivityIdentifier, latestVersion()))
  }

  @Test
  fun `returns the correct error for a non existing before activity`() {

    requestActivitiesWithBefore(task = task, before = randomUUID())
        .andExpectNotFound()
        .andExpect(hasMessage(translate(ACTIVITY_VALIDATION_ERROR_NOT_FOUND)))
  }

  private fun buildSummaryForTask() =
      buildSummary(
          messageKey = TASK_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
                  "task" to buildPlaceholder(task.getAggregateIdentifier(), task.getName())))

  private fun buildSummaryForDayCard() =
      buildSummary(
          messageKey = DAY_CARD_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
                  "daycard" to buildPlaceholder(getByReference("dayCard"), "Daycard Title")))

  private fun buildSummaryForTopic() =
      buildSummary(
          messageKey = TOPIC_ACTIVITY_CREATED_UNCRITICAL,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)),
                  "topic" to
                      buildPlaceholder(topic.getAggregateIdentifier(), topic.getDescription())))
}
