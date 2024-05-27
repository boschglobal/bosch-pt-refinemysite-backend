/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskListResource
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED as TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED as SCHEDULE_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskScheduleRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: TaskScheduleAggregateAvro

  lateinit var aggregateV1: TaskScheduleAggregateAvro

  lateinit var aggregateV2: TaskScheduleAggregateAvro

  val task by lazy { get<TaskAggregateAvro>("task")!! }

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query schedules with all parameters set`() {
    submitEvents(true)

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(3)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.eventTimestamp).isEqualTo(task.eventTimestamp())
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV1 = taskList.tasks[2]
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskWithScheduleV1.deleted).isFalse()
  }

  @Test
  fun `query schedules with all parameters set latest only`() {
    submitEvents(true)

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)

    val taskWithScheduleV1 = taskList.tasks.first()
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(LocalDate.now().plusDays(1))
    assertThat(taskWithScheduleV1.end).isEqualTo(LocalDate.now().plusDays(2))
    assertThat(taskWithScheduleV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskWithScheduleV1.deleted).isFalse()
  }

  @Test
  fun `query schedules with all parameters set multiple schedules`() {
    submitEvents(true)

    val expectedStartAfterDelete = LocalDate.now().plusDays(7)
    val expectedEndAfterDelete = LocalDate.now().plusDays(14)

    eventStreamGenerator.submitTaskSchedule(eventType = DELETED).submitTaskSchedule(
        "taskSchedule1") {
          it.start = expectedStartAfterDelete.toEpochMilli()
          it.end = expectedEndAfterDelete.toEpochMilli()
        }

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(5)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV1 = taskList.tasks[2]
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.deleted).isFalse()

    // Task schedule deleted
    val taskWithScheduleV2 = taskList.tasks[3]
    assertThat(taskWithScheduleV2.version).isEqualTo(0)
    assertThat(taskWithScheduleV2.start).isNull()
    assertThat(taskWithScheduleV2.end).isNull()
    assertThat(taskWithScheduleV2.deleted).isFalse()

    // Task with date from second schedule
    val taskWithSchedule1 = taskList.tasks[4]
    assertThat(taskWithSchedule1.version).isEqualTo(0)
    assertThat(taskWithSchedule1.start).isEqualTo(expectedStartAfterDelete)
    assertThat(taskWithSchedule1.end).isEqualTo(expectedEndAfterDelete)
    assertThat(taskWithSchedule1.deleted).isFalse()

    // Event timestamps must increase monotonically
    assertThat(taskList.tasks[0].eventTimestamp).isLessThan(taskList.tasks[1].eventTimestamp)
    assertThat(taskList.tasks[1].eventTimestamp).isLessThan(taskList.tasks[2].eventTimestamp)
    assertThat(taskList.tasks[2].eventTimestamp).isLessThan(taskList.tasks[3].eventTimestamp)
    assertThat(taskList.tasks[3].eventTimestamp).isLessThan(taskList.tasks[4].eventTimestamp)
  }

  @Test
  fun `query schedules with all parameters set latest only multiple schedules`() {
    submitEvents(true)

    val aggregateV2: TaskScheduleAggregateAvro =
        eventStreamGenerator
            .submitTaskSchedule(eventType = DELETED)
            .submitTaskSchedule("taskSchedule1") {
              it.start = LocalDate.now().plusDays(7).toEpochMilli()
              it.end = LocalDate.now().plusDays(14).toEpochMilli()
            }
            .get("taskSchedule1")!!

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)

    val taskWithSchedule1 = taskList.tasks.first()
    assertThat(taskWithSchedule1.version).isEqualTo(0)
    assertThat(taskWithSchedule1.start).isEqualTo(LocalDate.now().plusDays(7))
    assertThat(taskWithSchedule1.end).isEqualTo(LocalDate.now().plusDays(14))
    assertThat(taskWithSchedule1.eventTimestamp).isEqualTo(aggregateV2.eventTimestamp())
    assertThat(taskWithSchedule1.deleted).isFalse()
  }

  @Test
  fun `query schedule without optional parameters`() {
    submitEvents(false)

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)

    // Task without schedule as there's no date change that requires an additional version
    val task = taskList.tasks[0]
    assertThat(task.version).isEqualTo(0)
    assertThat(task.start).isNull()
    assertThat(task.end).isNull()
    assertThat(task.eventTimestamp).isEqualTo(this.task.eventTimestamp())
    assertThat(task.deleted).isFalse()
  }

  @Test
  fun `query schedule without optional parameters latest only`() {
    submitEvents(false)

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)

    // Task without schedule as there's no date change that requires an additional version
    val task = taskList.tasks.first()
    assertThat(task.version).isEqualTo(0)
    assertThat(task.start).isNull()
    assertThat(task.end).isNull()
    assertThat(task.eventTimestamp).isEqualTo(this.aggregateV1.eventTimestamp())
    assertThat(task.deleted).isFalse()
  }

  // Ensure that nothing has changed. Deleted schedules are still resolved.
  @Test
  fun `query deleted schedules`() {
    submitAsDeletedEvents()

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(4)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.eventTimestamp).isEqualTo(task.eventTimestamp())
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV1 = taskList.tasks[2]
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskWithScheduleV1.deleted).isFalse()

    // Task schedule deleted
    val taskWithScheduleV2 = taskList.tasks[3]
    assertThat(taskWithScheduleV2.version).isEqualTo(0)
    assertThat(taskWithScheduleV2.start).isNull()
    assertThat(taskWithScheduleV2.end).isNull()
    assertThat(taskWithScheduleV2.eventTimestamp).isEqualTo(aggregateV2.eventTimestamp())
    assertThat(taskWithScheduleV2.deleted).isFalse()
  }

  // Ensure that deleted schedules aren't returned.
  @Test
  fun `query deleted schedules latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val taskList = query(true)

    // Validate payload
    assertThat(taskList.tasks).hasSize(1)

    // Task create
    val task = taskList.tasks[0]
    assertThat(task.version).isEqualTo(0)
    assertThat(task.start).isNull()
    assertThat(task.end).isNull()
    assertThat(task.eventTimestamp).isEqualTo(this.task.eventTimestamp())
    assertThat(task.deleted).isFalse()
  }

  // Ensure that schedules have the correct version when the task version is > 0
  @Test
  fun `query schedules of different task versions`() {
    submitEvents(true)

    val taskBeforeUpdate: TaskAggregateAvro = get("task")!!

    val aggregateV2: TaskScheduleAggregateAvro =
        eventStreamGenerator
            .submitTask(eventType = TASK_UPDATED)
            .submitTaskSchedule(eventType = SCHEDULE_UPDATED) {
              it.start = LocalDate.now().plusDays(7).toEpochMilli()
              it.end = LocalDate.now().plusDays(14).toEpochMilli()
            }
            .get("taskSchedule")!!

    assertThat(getByReference("task").version).isEqualTo(1)

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(5)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.eventTimestamp).isEqualTo(taskBeforeUpdate.eventTimestamp())
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(aggregateV0.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.end).isEqualTo(aggregateV0.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV1 = taskList.tasks[2]
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(taskWithScheduleV1.deleted).isFalse()

    // Task update
    val taskWithScheduleV2 = taskList.tasks[3]
    assertThat(taskWithScheduleV2.version).isEqualTo(1)
    assertThat(taskWithScheduleV2.start).isEqualTo(aggregateV1.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV2.end).isEqualTo(aggregateV1.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV2.eventTimestamp).isEqualTo(task.eventTimestamp())
    assertThat(taskWithScheduleV2.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV3 = taskList.tasks[4]
    assertThat(taskWithScheduleV3.version).isEqualTo(1)
    assertThat(taskWithScheduleV3.start).isEqualTo(aggregateV2.start.toLocalDateByMillis())
    assertThat(taskWithScheduleV3.end).isEqualTo(aggregateV2.end.toLocalDateByMillis())
    assertThat(taskWithScheduleV3.eventTimestamp).isEqualTo(aggregateV2.eventTimestamp())
    assertThat(taskWithScheduleV3.deleted).isFalse()
  }

  @Test
  fun `query schedule after it was deleted and recreated`() {
    val expectedStartBeforeDelete = LocalDate.now()
    val expectedEndBeforeDelete = LocalDate.now().plusDays(2)

    val expectedStartAfterRecreation = LocalDate.now().plusDays(7)
    val expectedEndAfterRecreation = LocalDate.now().plusDays(9)

    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskSchedule {
          it.start = expectedStartBeforeDelete.toEpochMilli()
          it.end = expectedEndBeforeDelete.toEpochMilli()
        }
        .submitTaskSchedule(eventType = DELETED)
        .submitTaskSchedule(asReference = "recreatedSchedule") {
          it.start = expectedStartAfterRecreation.toEpochMilli()
          it.end = expectedEndAfterRecreation.toEpochMilli()
        }

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(4)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(expectedStartBeforeDelete)
    assertThat(taskWithScheduleV0.end).isEqualTo(expectedEndBeforeDelete)
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule delete
    val taskWithDeletedSchedule = taskList.tasks[2]
    assertThat(taskWithDeletedSchedule.version).isEqualTo(0)
    assertThat(taskWithDeletedSchedule.start).isNull()
    assertThat(taskWithDeletedSchedule.end).isNull()
    assertThat(taskWithDeletedSchedule.deleted).isFalse()

    // Task schedule recreate
    val taskWithRecreatedSchedule = taskList.tasks[3]
    assertThat(taskWithRecreatedSchedule.version).isEqualTo(0)
    assertThat(taskWithRecreatedSchedule.start).isEqualTo(expectedStartAfterRecreation)
    assertThat(taskWithRecreatedSchedule.end).isEqualTo(expectedEndAfterRecreation)
    assertThat(taskWithRecreatedSchedule.deleted).isFalse()

    // Event timestamps must increase monotonically
    assertThat(taskList.tasks[0].eventTimestamp).isLessThan(taskList.tasks[1].eventTimestamp)
    assertThat(taskList.tasks[1].eventTimestamp).isLessThan(taskList.tasks[2].eventTimestamp)
    assertThat(taskList.tasks[2].eventTimestamp).isLessThan(taskList.tasks[3].eventTimestamp)
  }

  @Test
  fun `changing only day card slots must not produce a task change`() {
    val expectedStart = LocalDate.now()
    val expectedEnd = LocalDate.now().plusDays(2)

    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskSchedule {
          it.start = expectedStart.toEpochMilli()
          it.end = expectedEnd.toEpochMilli()
        }
        .submitTaskScheduleSlotsChanged()

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(2)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(expectedStart)
    assertThat(taskWithScheduleV0.end).isEqualTo(expectedEnd)
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Event timestamps must increase monotonically
    assertThat(taskList.tasks[0].eventTimestamp).isLessThan(taskList.tasks[1].eventTimestamp)
  }

  private fun EventStreamGenerator.submitTaskScheduleSlotsChanged() =
      this.submitTaskSchedule(eventType = SCHEDULE_UPDATED) {
        it.slots.add(
            TaskScheduleSlotAvro(
                LocalDate.now().toEpochMilli(),
                AggregateIdentifierAvro(randomUUID().toString(), 0L, DAYCARD.name)))
      }

  @Test
  fun `changing card slots multiple times in a row must not produce task changes`() {
    val expectedStartAfterCreate = LocalDate.now()
    val expectedEndAfterCreate = LocalDate.now().plusDays(1)

    val expectedStartAfterUpdate = LocalDate.now().plusDays(2)
    val expectedEndAfterUpdate = LocalDate.now().plusDays(3)

    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskSchedule {
          it.start = expectedStartAfterCreate.toEpochMilli()
          it.end = expectedEndAfterCreate.toEpochMilli()
        }
        .submitTaskScheduleSlotsChanged()
        .submitTaskScheduleSlotsChanged()
        .submitTaskScheduleSlotsChanged()
        .submitTaskSchedule(eventType = SCHEDULE_UPDATED) {
          it.start = expectedStartAfterUpdate.toEpochMilli()
          it.end = expectedEndAfterUpdate.toEpochMilli()
        }

    // Execute query
    val taskList = query(false)

    // Validate payload
    assertThat(taskList.tasks).hasSize(3)

    // Task create
    val taskWithoutSchedule = taskList.tasks[0]
    assertThat(taskWithoutSchedule.version).isEqualTo(0)
    assertThat(taskWithoutSchedule.start).isNull()
    assertThat(taskWithoutSchedule.end).isNull()
    assertThat(taskWithoutSchedule.deleted).isFalse()

    // Task schedule create
    val taskWithScheduleV0 = taskList.tasks[1]
    assertThat(taskWithScheduleV0.version).isEqualTo(0)
    assertThat(taskWithScheduleV0.start).isEqualTo(expectedStartAfterCreate)
    assertThat(taskWithScheduleV0.end).isEqualTo(expectedEndAfterCreate)
    assertThat(taskWithScheduleV0.deleted).isFalse()

    // Task schedule update
    val taskWithScheduleV1 = taskList.tasks[2]
    assertThat(taskWithScheduleV1.version).isEqualTo(0)
    assertThat(taskWithScheduleV1.start).isEqualTo(expectedStartAfterUpdate)
    assertThat(taskWithScheduleV1.end).isEqualTo(expectedEndAfterUpdate)
    assertThat(taskWithScheduleV1.deleted).isFalse()

    // Event timestamps must increase monotonically
    assertThat(taskList.tasks[0].eventTimestamp).isLessThan(taskList.tasks[1].eventTimestamp)
    assertThat(taskList.tasks[1].eventTimestamp).isLessThan(taskList.tasks[2].eventTimestamp)
  }

  private fun submitEvents(includeOptionals: Boolean) {
    aggregateV0 =
        eventStreamGenerator
            .submitProject()
            .submitCsmParticipant()
            .submitProjectCraftG2()
            .submitTask()
            .submitTaskSchedule {
              if (includeOptionals) {
                it.start = LocalDate.now().toEpochMilli()
                it.end = LocalDate.now().toEpochMilli()
              } else {
                it.start = null
                it.end = null
              }
            }
            .get("taskSchedule")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTaskSchedule(eventType = SCHEDULE_UPDATED) {
              if (includeOptionals) {
                it.start = LocalDate.now().plusDays(1).toEpochMilli()
                it.end = LocalDate.now().plusDays(2).toEpochMilli()
              } else {
                it.start = null
                it.end = null
              }
            }
            .get("taskSchedule")!!
  }

  private fun submitAsDeletedEvents() {
    aggregateV0 =
        eventStreamGenerator
            .submitProject()
            .submitCsmParticipant()
            .submitProjectCraftG2()
            .submitTask()
            .submitTaskSchedule {
              it.start = LocalDate.now().toEpochMilli()
              it.end = LocalDate.now().toEpochMilli()
            }
            .get("taskSchedule")!!

    aggregateV1 =
        eventStreamGenerator
            .submitTaskSchedule(eventType = SCHEDULE_UPDATED) {
              it.start = LocalDate.now().plusDays(1).toEpochMilli()
              it.end = LocalDate.now().plusDays(2).toEpochMilli()
            }
            .get("taskSchedule")!!

    aggregateV2 = eventStreamGenerator.submitTaskSchedule(eventType = DELETED).get("taskSchedule")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(latestProjectApi("/projects/tasks"), latestOnly, TaskListResource::class.java)
}
