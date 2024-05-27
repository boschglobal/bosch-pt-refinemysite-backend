/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskcopy.facade.rest.resource.request.CopyTaskResource
import com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto.OverridableTaskParametersDto
import com.bosch.pt.iot.smartsite.project.taskcopy.util.TaskCopyTestUtil.assertCreatedTaskMatchCopiedTask
import com.bosch.pt.iot.smartsite.project.taskcopy.util.TaskCopyTestUtil.assertCreatedTaskScheduleMatchCopiedTaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.toAggregateReference
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class TaskCopyIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskCopyController

  @Autowired private lateinit var taskScheduleRepository: TaskScheduleRepository

  private val task1 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task1").asTaskId())!!
  }
  private val schedule1 by lazy {
    repositories.findTaskScheduleWithDetails(getIdentifier("schedule1").asTaskScheduleId())!!
  }
  private val task2 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task2").asTaskId())!!
  }
  private val schedule2 by lazy {
    repositories.findTaskScheduleWithDetails(getIdentifier("schedule2").asTaskScheduleId())!!
  }
  private val task3 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task3").asTaskId())!!
  }
  private val schedule3 by lazy {
    repositories.findTaskScheduleWithDetails(getIdentifier("schedule3").asTaskScheduleId())!!
  }
  private val task4 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task4").asTaskId())!!
  }
  private val schedule4 by lazy {
    repositories.findTaskScheduleWithDetails(getIdentifier("schedule4").asTaskScheduleId())!!
  }
  private val task5 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task5").asTaskId())!!
  }
  private val workArea1 by lazy {
    repositories.findWorkArea(getIdentifier("workArea1").asWorkAreaId())!!
  }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task1") { it.name = "task1" }
        .submitTaskSchedule(asReference = "schedule1") {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
        }
        .submitDayCardG2(asReference = "dayCard1Task1") { it.status = DONE }
        .submitTaskSchedule(asReference = "schedule1", eventType = UPDATED) {
          it.slots = listOf(getByReference("dayCard1Task1").asSlot(LocalDate.now()))
        }
        .submitTask(asReference = "task2") {
          it.name = "task2"
          it.workarea = getIdentifier("workArea").asWorkAreaId().toAggregateReference()
        }
        .submitTaskSchedule(asReference = "schedule2") {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
        }
        .submitDayCardG2(asReference = "dayCard1Task2") {
          it.title = "dayCard1Task2"
          it.status = DONE
        }
        .submitDayCardG2(asReference = "dayCard2Task2") {
          it.title = "dayCard2Task2"
          it.status = NOTDONE
        }
        .submitTaskSchedule(asReference = "schedule2", eventType = UPDATED) {
          it.slots =
              listOf(
                  getByReference("dayCard1Task2").asSlot(LocalDate.now()),
                  getByReference("dayCard2Task2").asSlot(LocalDate.now().plusDays(1)))
        }
        .submitTask(asReference = "task3") { it.name = "task3" }
        .submitTaskSchedule(asReference = "schedule3") {
          it.start = LocalDate.now().toEpochMilli()
          it.end = null
        }
        .submitTask(asReference = "task4") { it.name = "task4" }
        .submitTaskSchedule(asReference = "schedule4") {
          it.start = null
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
        }
        .submitTask(asReference = "task5") { it.name = "task5" }
        .submitWorkArea(asReference = "workArea1")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify copy tasks succeed for includeDayCards at true and false`() {
    val resource =
        CreateBatchRequestResource(
            listOf(
                CopyTaskResource(id = task1.identifier, shiftDays = 7, includeDayCards = false),
                CopyTaskResource(id = task2.identifier, shiftDays = 7, includeDayCards = true)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask1 =
        repositories.findTaskWithDetails(responseToTaskName[task1.name]!!.id.asTaskId())!!
    val copyOfTask2 =
        repositories.findTaskWithDetails(responseToTaskName[task2.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask1, task1)
    assertCreatedTaskMatchCopiedTask(copyOfTask2, task2)

    val copyOfSchedule1 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task1.name]!!.id.asTaskId())!!
    val copyOfSchedule2 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task2.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule1, schedule1, 7, false)
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule2, schedule2, 7, true)

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            TaskScheduleEventAvro::class.java,
            // For each Schedule:
            // - Create Day Cards Batch Operations
            // - Add of each Day Card to Schedule
            DayCardEventG2Avro::class.java,
            DayCardEventG2Avro::class.java,
            TaskScheduleEventAvro::class.java,
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy tasks succeed with day cards and work area override to given work area id`() {
    val parametersDto =
        OverridableTaskParametersDto(workAreaId = WorkAreaIdOrEmpty(workArea1.identifier))
    val resource =
        CreateBatchRequestResource(
            listOf(
                CopyTaskResource(
                    id = task1.identifier,
                    shiftDays = 7,
                    includeDayCards = true,
                    parametersOverride = parametersDto)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask1 =
        repositories.findTaskWithDetails(responseToTaskName[task1.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask1, task1, parametersDto)

    val copyOfSchedule1 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task1.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule1, schedule1, 7, true)

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // For each Schedule:
            // - Create Day Cards Batch Operations
            // - Add of each Day Card to Schedule
            DayCardEventG2Avro::class.java,
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy tasks succeed with day cards and work area override to null`() {
    val parametersDto = OverridableTaskParametersDto(workAreaId = WorkAreaIdOrEmpty())
    val resource =
        CreateBatchRequestResource(
            listOf(
                CopyTaskResource(
                    id = task2.identifier,
                    shiftDays = 7,
                    includeDayCards = true,
                    parametersOverride = parametersDto)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask2 =
        repositories.findTaskWithDetails(responseToTaskName[task2.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask2, task2, parametersDto)

    val copyOfSchedule2 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task2.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule2, schedule2, 7, true)

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // For each Schedule:
            // - Create Day Cards Batch Operations
            // - Add of each Day Card to Schedule
            DayCardEventG2Avro::class.java,
            DayCardEventG2Avro::class.java,
            TaskScheduleEventAvro::class.java,
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy tasks succeed without end date`() {
    val resource =
        CreateBatchRequestResource(
            listOf(CopyTaskResource(id = task3.identifier, shiftDays = 7, includeDayCards = true)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask3 =
        repositories.findTaskWithDetails(responseToTaskName[task3.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask3, task3)

    val copyOfSchedule3 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task3.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule3, schedule3, 7, true)

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy task succeeds without start date`() {
    val resource =
        CreateBatchRequestResource(
            listOf(CopyTaskResource(id = task4.identifier, shiftDays = 7, includeDayCards = true)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask4 =
        repositories.findTaskWithDetails(responseToTaskName[task4.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask4, task4)

    val copyOfSchedule4 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task4.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule4, schedule4, 7, true)

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy tasks succeed without schedule`() {
    val resource =
        CreateBatchRequestResource(
            listOf(
                CopyTaskResource(id = task4.identifier, shiftDays = 7, includeDayCards = true),
                CopyTaskResource(id = task5.identifier, shiftDays = 7, includeDayCards = true)))

    val response = cut.copy(projectIdentifier, resource).body!!.items
    val responseToTaskName = response.associateBy { it.name }

    // Test Database
    val copyOfTask4 =
        repositories.findTaskWithDetails(responseToTaskName[task4.name]!!.id.asTaskId())!!
    val copyOfTask5 =
        repositories.findTaskWithDetails(responseToTaskName[task5.name]!!.id.asTaskId())!!
    assertCreatedTaskMatchCopiedTask(copyOfTask4, task4)
    assertCreatedTaskMatchCopiedTask(copyOfTask5, task5)

    val copyOfSchedule4 =
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            responseToTaskName[task4.name]!!.id.asTaskId())!!
    assertCreatedTaskScheduleMatchCopiedTaskSchedule(copyOfSchedule4, schedule4, 7, true)
    assertThat(
            taskScheduleRepository.findWithDetailsByTaskIdentifier(
                responseToTaskName[task5.name]!!.id.asTaskId()))
        .isNull()

    // Test Events
    projectEventStoreUtils.verifyContainsInSequence(
        listOf(
            // Copy Batch Operation Start
            BatchOperationStartedEventAvro::class.java,
            // Create Task Batch Operations
            TaskEventAvro::class.java,
            TaskEventAvro::class.java,
            // Create Schedules Batch Operations
            TaskScheduleEventAvro::class.java,
            // Copy Batch Operation Finish
            BatchOperationFinishedEventAvro::class.java))
  }

  @Test
  fun `verify copy tasks fails for tasks from different projects`() {
    eventStreamGenerator
        .submitProject(asReference = "anotherProject")
        .submitWorkdayConfiguration()
        .submitWorkArea()
        .submitWorkAreaList()
        .submitParticipantG3(asReference = "participantCsmFromAnotherProject") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitProjectCraftG2()
        .submitProjectCraftList()
        .submitTask(asReference = "taskFromAnotherProject")

    val resource =
        CreateBatchRequestResource(
            listOf(
                CopyTaskResource(id = task1.identifier, shiftDays = 7, includeDayCards = true),
                CopyTaskResource(
                    id = getIdentifier("taskFromAnotherProject").asTaskId(),
                    shiftDays = 7,
                    includeDayCards = true)))

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.copy(projectIdentifier, resource) }
        .withMessage(
            "The task identifiers to copy were not found or are not from the given project")

    // Test Events
    projectEventStoreUtils.verifyEmpty()
  }
}
