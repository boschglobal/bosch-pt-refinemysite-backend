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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_REMOVED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_DESCRIPTION_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_LOCATION_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_STATUS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskUpdatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @Nested
  inner class `when adding` {

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
    fun `the working area`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.workarea = getByReference("workArea1")
      }

      requestActivities(task)
          .andExpectOk()
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_CREATED_WORKAREA, "workArea1")))
    }

    @Test
    fun `the location`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.location = "location" }

      requestActivities(task)
          .andExpectOk()
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_CREATED_LOCATION, "location")))
    }

    @Test
    fun `the description`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.description = "description" }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_CREATED_DESCRIPTION, "description")))
    }
  }

  @Nested
  inner class `when changing` {

    @BeforeEach
    fun init() {
      eventStreamGenerator.setUserContext("fm-user").submitTask {
        it.assignee = getByReference("fm-participant")
        it.workarea = getByReference("workArea1")
        it.name = "task"
        it.location = "location"
        it.description = "description"
        it.status = DRAFT
      }
    }

    @Test
    fun `the project craft`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.craft = getByReference("projectCraft2")
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(
              hasChange(translate(TASK_ACTIVITY_UPDATED_CRAFT, "projectCraft1", "projectCraft2")))
    }

    @Test
    fun `the working area`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.workarea = getByReference("workArea2")
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_WORKAREA, "workArea1", "workArea2")))
    }

    @Test
    fun `the location`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.location = "changed" }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_LOCATION, "location", "changed")))
    }

    @Test
    fun `the description`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.description = "changed" }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_DESCRIPTION, "changed")))
    }

    @Test
    fun `the name`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.name = "changed" }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_NAME, "task", "changed")))
    }

    @Test
    fun `the assignee`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.assignee = crParticipant.getAggregateIdentifier()
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(
              hasChange(
                  translate(
                      TASK_ACTIVITY_UPDATED_ASSIGNEE, displayName(fmUser), displayName(crUser))))
    }

    @Test
    fun `the status`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) { it.status = OPEN }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_STATUS, OPEN.toString())))
    }
  }

  @Nested
  inner class `when removing` {

    @BeforeEach
    fun init() {
      eventStreamGenerator.setUserContext("fm-user").submitTask {
        it.assignee = getByReference("fm-participant")
        it.workarea = getByReference("workArea1")
        it.name = "task"
        it.location = "location"
        it.description = "description"
        it.status = DRAFT
      }
    }

    @Test
    fun `the description`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.assignee = getByReference("fm-participant")
        it.description = null
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_DESCRIPTION_REMOVED, "description")))
    }

    @Test
    fun `the location`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.assignee = getByReference("fm-participant")
        it.location = null
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_UPDATED_LOCATION_REMOVED, "location")))
    }

    @Test
    fun `the working area`() {
      eventStreamGenerator.submitTask(eventType = UPDATED) {
        it.assignee = getByReference("fm-participant")
        it.workarea = null
      }

      requestActivities(task)
          .andExpect(hasId(findLatestActivity().identifier))
          .andExpect(hasDate(timeLineGenerator.time))
          .andExpect(hasUser(fmUser))
          .andExpect(hasSummary(buildSummary(TASK_ACTIVITY_UPDATED)))
          .andExpect(hasChange(translate(TASK_ACTIVITY_REMOVED_WORKAREA, "workArea1")))
    }
  }

  private fun buildSummary(messageKey: String, changesCount: Int = 1) =
      buildSummary(
          messageKey = messageKey,
          namedArguments = mapOf("count" to changesCount.toString()),
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), displayName(fmUser))))
}
