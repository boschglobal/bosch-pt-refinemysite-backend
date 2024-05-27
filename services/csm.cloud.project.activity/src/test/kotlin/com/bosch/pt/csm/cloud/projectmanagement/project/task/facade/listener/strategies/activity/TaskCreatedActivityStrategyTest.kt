/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChange
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_STATUS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskCreatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("cr-user")
  }

  @Test
  fun `when task is created with optional fields set to null`() {
    eventStreamGenerator.submitTask {
      it.name = "createdTask"
      it.status = DRAFT
      it.description = null
      it.location = null
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_CREATED)))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_CRAFT, projectCraft1.getName()),
                changeIndex = 0))
        .andExpect(
            hasChange(text = translate(TASK_ACTIVITY_CREATED_NAME, "createdTask"), changeIndex = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_STATUS, DRAFT.toString()), changeIndex = 2))
  }

  @Test
  fun `when task is created with optional fields set`() {
    eventStreamGenerator.submitTask {
      it.assignee = getByReference("fm-participant")
      it.workarea = getByReference("workArea1")
      it.description = "description1"
      it.location = "location1"
      it.name = "createdTask"
      it.status = DRAFT
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_CREATED)))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_ASSIGNEE, fmUser.displayName()),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_CRAFT, projectCraft1.getName()),
                changeIndex = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_DESCRIPTION, "description1"),
                changeIndex = 2))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_LOCATION, "location1"), changeIndex = 3))
        .andExpect(
            hasChange(text = translate(TASK_ACTIVITY_CREATED_NAME, "createdTask"), changeIndex = 4))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_STATUS, DRAFT.toString()), changeIndex = 5))
        .andExpect(
            hasChange(
                text = translate(TASK_ACTIVITY_CREATED_WORKAREA, workArea1.getName()),
                changeIndex = 6))
  }

  private fun buildSummary(messageKey: String) =
      buildSummary(
          messageKey = messageKey,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          crParticipant.getAggregateIdentifier(), crUser.displayName()),
                  "task" to buildPlaceholder(task.getAggregateIdentifier(), task.getName())))
}
