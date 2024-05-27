/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChange
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChangesCount
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_START
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.projectmanagement.util.formatForCurrentLocale
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskScheduleDeletedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  private val startDate = LocalDate.now()

  private val expectedStartDate = startDate.formatForCurrentLocale()

  private val endDate = LocalDate.now().plusDays(8)

  private val expectedEndDate = endDate.formatForCurrentLocale()

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
    }
  }

  @Test
  fun `when a task schedule is deleted with start and end date`() {
    eventStreamGenerator
        .submitTaskSchedule {
          it.start = startDate.atStartOfDay(UTC).toInstant().toEpochMilli()
          it.end = endDate.atStartOfDay(UTC).toInstant().toEpochMilli()
        }
        .submitTaskSchedule(eventType = DELETED)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 2))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_DELETED_START, expectedStartDate),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_DELETED_END, expectedEndDate),
                changeIndex = 1))
  }

  @Test
  fun `when a task schedule is deleted with only start date`() {
    eventStreamGenerator
        .submitTaskSchedule {
          it.start = startDate.atStartOfDay(UTC).toInstant().toEpochMilli()
          it.end = null
        }
        .submitTaskSchedule(eventType = DELETED)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_DELETED_START, expectedStartDate),
                changeIndex = 0))
  }

  @Test
  fun `when a task schedule is deleted with only end date`() {
    eventStreamGenerator
        .submitTaskSchedule {
          it.start = null
          it.end = endDate.atStartOfDay(UTC).toInstant().toEpochMilli()
        }
        .submitTaskSchedule(eventType = DELETED)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_DELETED_END, expectedEndDate),
                changeIndex = 0))
  }

  private fun getTaskScheduleLastModifiedDate() =
      get<TaskScheduleAggregateAvro>("taskSchedule")!!.getLastModifiedDate()

  private fun buildSummary() =
      buildSummary(
          messageKey = TASK_SCHEDULE_ACTIVITY_DELETED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
              ))
}
