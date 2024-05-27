/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities.asLocalDate
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.util.getIdentifier
import java.time.LocalDate
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreTaskScheduleSnapshotTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  fun `validate that schedule created event was processed successfully`() {
    val taskScheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!

    transactionTemplate.executeWithoutResult {
      val taskSchedule =
          repositories.findTaskScheduleWithDetails(
              taskScheduleAggregate.getIdentifier().asTaskScheduleId())!!

      validateBasicAttributes(taskSchedule, taskScheduleAggregate)
      validateAuditingInformationAndIdentifierAndVersion(taskSchedule, taskScheduleAggregate)
    }
  }

  @Test
  fun `validate that schedule updated event was processed successfully`() {
    eventStreamGenerator.submitDayCardG2().submitTaskSchedule(eventType = UPDATED) {
      it.start = now().toEpochMilli()
      it.end = now().plusDays(7).toEpochMilli()
      it.slots = listOf(TaskScheduleSlotAvro(now().toEpochMilli(), getByReference("dayCard")))
    }

    val taskScheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!

    transactionTemplate.executeWithoutResult {
      val taskSchedule =
          repositories.findTaskScheduleWithDetails(
              taskScheduleAggregate.getIdentifier().asTaskScheduleId())!!

      validateBasicAttributes(taskSchedule, taskScheduleAggregate)
      validateAuditingInformationAndIdentifierAndVersion(taskSchedule, taskScheduleAggregate)
    }
  }

  @Test
  fun `validate schedule deleted event deletes a task schedule`() {
    val taskScheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!

    assertThat(
            repositories.findTaskScheduleWithDetails(
                taskScheduleAggregate.getIdentifier().asTaskScheduleId()))
        .isNotNull

    eventStreamGenerator.submitTaskSchedule(eventType = DELETED)

    assertThat(
            repositories.findTaskScheduleWithDetails(
                taskScheduleAggregate.getIdentifier().asTaskScheduleId()))
        .isNull()

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  @Test
  fun `validate repeated event sequence is skipped if entities in database are newer`() {
    val startDate = now()
    val endDate = startDate.plusDays(10)
    eventStreamGenerator
        // Add the first day card
        .submitDayCardG2()
        .submitTaskSchedule(eventType = UPDATED) {
          it.start = startDate.toEpochMilli()
          it.end = endDate.toEpochMilli()
          it.slots =
              listOf(TaskScheduleSlotAvro(startDate.toEpochMilli(), getByReference("dayCard")))
        }
        // Create a second day card and add it to the task schedule
        .submitDayCardG2("dayCard2")

    val dayCardAggregateIdentifier = getByReference("dayCard")
    val dayCard2AggregateIdentifier = getByReference("dayCard2")

    eventStreamGenerator.submitTaskSchedule(eventType = UPDATED) {
      it.slots =
          listOf(
              getSlot(startDate, 0, dayCardAggregateIdentifier),
              getSlot(startDate, 1, dayCard2AggregateIdentifier),
          )
    }

    val expectedIdentifier = getByReference("taskSchedule")
    assertThat(expectedIdentifier.version).isEqualTo(2)

    // Create a third day card and add it to the task schedule
    eventStreamGenerator.submitDayCardG2("dayCard3")

    val dayCard3AggregateIdentifier = getByReference("dayCard3")

    eventStreamGenerator.submitTaskSchedule(eventType = UPDATED) {
      it.slots =
          listOf(
              getSlot(startDate, 0, dayCardAggregateIdentifier),
              getSlot(startDate, 1, dayCard2AggregateIdentifier),
              getSlot(startDate, 2, dayCard3AggregateIdentifier),
          )
    }

    val taskScheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!

    transactionTemplate.executeWithoutResult {
      val taskSchedule =
          repositories.findTaskScheduleWithDetails(
              taskScheduleAggregate.getIdentifier().asTaskScheduleId())!!

      validateBasicAttributes(taskSchedule, taskScheduleAggregate)
      validateAuditingInformationAndIdentifierAndVersion(taskSchedule, taskScheduleAggregate)
    }

    // Remove day card 2 from the task schedule and delete the day card
    eventStreamGenerator
        .submitTaskSchedule(eventType = UPDATED) {
          it.slots =
              listOf(
                  getSlot(startDate, 0, dayCardAggregateIdentifier),
                  getSlot(startDate, 2, dayCard3AggregateIdentifier),
              )
        }
        .submitDayCardG2("dayCard2", eventType = DayCardEventEnumAvro.DELETED)

    val expectedTaskScheduleAggregate = get<TaskScheduleAggregateAvro>("taskSchedule")!!
    assertThat(expectedTaskScheduleAggregate.getVersion()).isEqualTo(4)
    assertThat(repositories.dayCardRepository.findAll()).hasSize(2)

    // Repeat the sequence from the point where day card 2 is added to the schedule (last 5 events).
    // This step normally requires that day card still exists (but it was removed previously).
    // Expects that the event is skipped since the task schedule in the db is already newer.

    assertThat(expectedIdentifier.version).isEqualTo(2)
    eventStreamGenerator.repeat(5)

    eventStreamGenerator.submitTaskSchedule(eventType = UPDATED) {
      it.aggregateIdentifier = expectedIdentifier
      it.slots =
          listOf(
              getSlot(startDate, 0, dayCardAggregateIdentifier),
              getSlot(startDate, 1, dayCard2AggregateIdentifier),
          )
    }

    // Validate that nothing has changed
    transactionTemplate.executeWithoutResult {
      val taskSchedule =
          repositories.taskScheduleRepository.findWithDetailsByIdentifier(
              taskScheduleAggregate.getIdentifier().asTaskScheduleId())!!

      assertThat(taskSchedule.version).isEqualTo(4)

      validateBasicAttributes(taskSchedule, expectedTaskScheduleAggregate)
      validateAuditingInformationAndIdentifierAndVersion(
          taskSchedule, expectedTaskScheduleAggregate)
    }
  }

  private fun validateBasicAttributes(
      taskSchedule: TaskSchedule,
      taskScheduleAggregate: TaskScheduleAggregateAvro
  ) {
    assertThat(taskSchedule.task!!.identifier)
        .isEqualTo(taskScheduleAggregate.task.identifier.asTaskId())
    assertThat(taskSchedule.end).isEqualTo(asLocalDate(taskScheduleAggregate.end))
    assertThat(taskSchedule.start).isEqualTo(asLocalDate(taskScheduleAggregate.start))
    if (taskScheduleAggregate.slots != null) {
      val aggregateSlots =
          taskScheduleAggregate.slots.map {
            Pair(asLocalDate(it.date), it.dayCard.identifier.asDayCardId())
          }
      val entitySlots = taskSchedule.slots!!.map { Pair(it.date, it.dayCard!!.identifier) }
      assertThat(entitySlots).isEqualTo(aggregateSlots)
    }
  }

  private fun getSlot(
      startDate: LocalDate,
      offsetDays: Long,
      dayCardAggregateIdenfifier: AggregateIdentifierAvro
  ): TaskScheduleSlotAvro {
    return TaskScheduleSlotAvro.newBuilder()
        .setDate(startDate.plusDays(offsetDays).toEpochMilli())
        .setDayCard(dayCardAggregateIdenfifier)
        .build()
  }
}
