/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.extensions.toList
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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_CREATED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_CREATED_START
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_START
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_ADDED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_START
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import com.bosch.pt.csm.cloud.projectmanagement.util.formatForCurrentLocale
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.Locale.ENGLISH
import java.util.Locale.GERMAN
import java.util.Locale.setDefault
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity when a task schedule is updated")
@SmartSiteSpringBootTest
class TaskScheduleUpdatedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  private val startDate = LocalDate.now()

  private val expectedStartDate = startDate.formatForCurrentLocale()

  private val endDate = LocalDate.now().plusDays(8)

  private val expectedEndDate = endDate.formatForCurrentLocale()

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .submitDayCardG2 {
          it.title = "Daycard Title"
          it.manpower = 1F.toBigDecimal()
          it.reason = null
        }
  }

  @Test
  fun `adding the start date`() {

    submitTaskScheduleCreatedEvent(end = endDate)
    submitTaskScheduleUpdatedEvent(start = startDate, end = endDate)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_CREATED_START, expectedStartDate),
                changeIndex = 0))
  }

  @Test
  fun `adding the end date`() {

    submitTaskScheduleCreatedEvent(start = startDate)
    submitTaskScheduleUpdatedEvent(start = startDate, end = endDate)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text = translate(TASK_SCHEDULE_ACTIVITY_CREATED_END, expectedEndDate),
                changeIndex = 0))
  }

  @Test
  fun ` with changing the start date`() {

    val newStartDate = LocalDate.now().plusDays(1)
    val expectedNewStartDate = newStartDate.formatForCurrentLocale()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate)
    submitTaskScheduleUpdatedEvent(start = newStartDate, end = endDate)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_START,
                        expectedStartDate,
                        expectedNewStartDate),
                changeIndex = 0))
  }

  @Test
  fun `with changing the end date`() {

    val newEndDate = LocalDate.now().plusDays(14)
    val expectedNewEndDate = newEndDate.formatForCurrentLocale()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate)
    submitTaskScheduleUpdatedEvent(start = startDate, end = newEndDate)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_END, expectedEndDate, expectedNewEndDate),
                changeIndex = 0))
  }

  @Test
  fun `with removing the start date`() {

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate)
    submitTaskScheduleUpdatedEvent(end = endDate)

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
  fun `with removing the end date`() {

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate)
    submitTaskScheduleUpdatedEvent(start = startDate)

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

  @Test
  fun `changing start and end date, as well as, reordering existing dates of daycard slots`() {

    val newStartDate = LocalDate.now().plusDays(1)
    val expectedNewStartDate = newStartDate.formatForCurrentLocale()

    val newEndDate = LocalDate.now().plusDays(14)
    val expectedNewEndDate = newEndDate.formatForCurrentLocale()

    val dayCardDate = LocalDate.now().plusDays(1)
    val expectedDayCardDate = dayCardDate.formatForCurrentLocale()

    val newDayCardDate = LocalDate.now().plusDays(2)
    val expectedNewDayCardDate = newDayCardDate.formatForCurrentLocale()

    val slots = newSlot(dayCardDate).toList()
    val newSlots = newSlot(newDayCardDate).toList()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate, slots = slots)
    submitTaskScheduleUpdatedEvent(start = newStartDate, end = newEndDate, slots = newSlots)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 3))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_START,
                        expectedStartDate,
                        expectedNewStartDate),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_END, expectedEndDate, expectedNewEndDate),
                changeIndex = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED,
                        "Daycard Title",
                        expectedDayCardDate,
                        expectedNewDayCardDate),
                changeIndex = 2))
  }

  @Test
  fun `changing start and end date, as well as, reordering existing dates of daycard slots for a GERMAN locale`() {

    setDefault(GERMAN)

    val expectedStartDateGerman = startDate.formatForCurrentLocale()

    val expectedEndDateGerman = endDate.formatForCurrentLocale()

    val newStartDate = LocalDate.now().plusDays(1)
    val expectedNewStartDate = newStartDate.formatForCurrentLocale()

    val newEndDate = LocalDate.now().plusDays(14)
    val expectedNewEndDate = newEndDate.formatForCurrentLocale()

    val dayCardDate = LocalDate.now().plusDays(1)
    val expectedDayCardDate = dayCardDate.formatForCurrentLocale()

    val newDayCardDate = LocalDate.now().plusDays(2)
    val expectedNewDayCardDate = newDayCardDate.formatForCurrentLocale()

    val slots = newSlot(dayCardDate).toList()
    val newSlots = newSlot(newDayCardDate).toList()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate, slots = slots)
    submitTaskScheduleUpdatedEvent(start = newStartDate, end = newEndDate, slots = newSlots)

    requestActivities(task)
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 3))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_START,
                        expectedStartDateGerman,
                        expectedNewStartDate),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_END,
                        expectedEndDateGerman,
                        expectedNewEndDate),
                changeIndex = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED,
                        "Daycard Title",
                        expectedDayCardDate,
                        expectedNewDayCardDate),
                changeIndex = 2))

    // Return the default language to english
    setDefault(ENGLISH)
  }

  @Test
  fun `adding a new daycard slot`() {

    val dayCardDate = LocalDate.now().plusDays(1)
    val expectedDayCardDate = dayCardDate.formatForCurrentLocale()

    val newSlots = newSlot(dayCardDate).toList()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate)
    submitTaskScheduleUpdatedEvent(start = startDate, end = endDate, slots = newSlots)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_ADDED,
                        "Daycard Title",
                        expectedDayCardDate),
                changeIndex = 0))
  }

  @Test
  fun `removing a existing daycard slot`() {

    val dayCardDate = LocalDate.now().plusDays(1)
    val expectedDayCardDate = dayCardDate.formatForCurrentLocale()

    val slots = newSlot(dayCardDate).toList()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate, slots = slots)
    submitTaskScheduleUpdatedEvent(start = startDate, end = endDate)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REMOVED,
                        "Daycard Title",
                        expectedDayCardDate),
                changeIndex = 0))
  }

  @Test
  fun `reordering existing dates of daycard slots`() {

    val dayCardDate = LocalDate.now().plusDays(1)
    val expectedDayCardDate = dayCardDate.formatForCurrentLocale()

    val newDayCardDate = LocalDate.now().plusDays(2)
    val expectedNewDayCardDate = newDayCardDate.formatForCurrentLocale()

    val slots = newSlot(dayCardDate).toList()
    val newSlots = newSlot(newDayCardDate).toList()

    submitTaskScheduleCreatedEvent(start = startDate, end = endDate, slots = slots)
    submitTaskScheduleUpdatedEvent(start = startDate, end = endDate, slots = newSlots)

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(getTaskScheduleLastModifiedDate()))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasChangesCount(count = 1))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED,
                        "Daycard Title",
                        expectedDayCardDate,
                        expectedNewDayCardDate),
                changeIndex = 0))
  }

  private fun newSlot(dayCardDate: LocalDate) =
      TaskScheduleSlotAvro.newBuilder()
          .apply {
            dayCard = getByReference("dayCard")
            date = dayCardDate.atStartOfDay(UTC).toInstant().toEpochMilli()
          }
          .build()

  private fun submitTaskScheduleCreatedEvent(
      start: LocalDate? = null,
      end: LocalDate? = null,
      slots: List<TaskScheduleSlotAvro>? = emptyList()
  ) =
      eventStreamGenerator.submitTaskSchedule {
        it.start = start?.atStartOfDay(UTC)?.toInstant()?.toEpochMilli()
        it.end = end?.atStartOfDay(UTC)?.toInstant()?.toEpochMilli()
        it.slots = slots
      }

  private fun submitTaskScheduleUpdatedEvent(
      start: LocalDate? = null,
      end: LocalDate? = null,
      slots: List<TaskScheduleSlotAvro>? = emptyList()
  ) =
      eventStreamGenerator.submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
        it.start = start?.atStartOfDay(UTC)?.toInstant()?.toEpochMilli()
        it.end = end?.atStartOfDay(UTC)?.toInstant()?.toEpochMilli()
        it.slots = slots
      }

  private fun getTaskScheduleLastModifiedDate() =
      eventStreamGenerator.get<TaskScheduleAggregateAvro>("taskSchedule")!!.getLastModifiedDate()

  private fun buildSummary() =
      buildSummary(
          messageKey = TASK_SCHEDULE_ACTIVITY_UPDATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName()),
              ))
}
