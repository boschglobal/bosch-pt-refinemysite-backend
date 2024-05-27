/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasNoChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_FINISHED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_SENT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_SENT_WITHOUT_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_STARTED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskStatusChangedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("cr-user").submitTask {
      it.craft = getByReference("projectCraft1")
    }
  }

  @Test
  fun `when a task is started`() {
    eventStreamGenerator.submitTask(eventType = STARTED) {
      it.assignee = getByReference("fm-participant")
      it.status = TaskStatusEnumAvro.STARTED
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_STARTED, crParticipant, crUser)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when a task is sent`() {
    eventStreamGenerator.submitTask(eventType = SENT) {
      it.assignee = getByReference("fm-participant")
      it.status = TaskStatusEnumAvro.OPEN
    }

    val expectedSummary =
        buildSummary(
            TASK_ACTIVITY_SENT,
            objectReferences =
                mapOf(
                    "originator" to
                        buildPlaceholder(
                            crParticipant.getAggregateIdentifier(), crUser.displayName()),
                    "assignee" to
                        buildPlaceholder(
                            fmParticipant.getAggregateIdentifier(), fmUser.displayName())))

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(expectedSummary))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when a task is sent without assignee`() {
    eventStreamGenerator.submitTask(eventType = SENT) { it.status = TaskStatusEnumAvro.OPEN }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(
            hasSummary(buildSummary(TASK_ACTIVITY_SENT_WITHOUT_ASSIGNEE, crParticipant, crUser)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when a task is closed`() {
    eventStreamGenerator.submitTask(eventType = CLOSED) {
      it.assignee = getByReference("fm-participant")
      it.status = TaskStatusEnumAvro.CLOSED
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_FINISHED, crParticipant, crUser)))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `when a task is accepted`() {
    eventStreamGenerator.setUserContext("csm-user").submitTask(eventType = ACCEPTED) {
      it.assignee = getByReference("fm-participant")
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(csmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_ACCEPTED, csmParticipant, csmUser)))
        .andExpect(hasNoChanges())
  }

  private fun buildSummary(
      messageKey: String,
      participant: ParticipantAggregateG3Avro,
      user: UserAggregateAvro
  ) =
      buildSummary(
          messageKey = messageKey,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(participant.getAggregateIdentifier(), user.displayName())))
}
