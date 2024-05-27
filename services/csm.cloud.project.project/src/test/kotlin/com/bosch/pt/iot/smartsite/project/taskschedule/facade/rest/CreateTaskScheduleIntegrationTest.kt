/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_ALREADY_EXISTS
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.CreateTaskScheduleResource
import com.bosch.pt.iot.smartsite.util.withMessageKey
import jakarta.validation.ConstraintViolationException
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
class CreateTaskScheduleIntegrationTest : AbstractTaskScheduleIntegrationTest() {

  @Test
  fun `verify create task schedule with start and end`() {
    val taskSchedule =
        cut.createTaskSchedule(
            taskWithoutSchedule.identifier,
            CreateTaskScheduleResource(defaultStartDate, defaultEndDate))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isEqualTo(defaultStartDate)
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultEndDate)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, CREATED, 1)
  }

  @Test
  fun `verify create task schedule with start`() {
    val taskSchedule =
        cut.createTaskSchedule(
            taskWithoutSchedule.identifier, CreateTaskScheduleResource(defaultStartDate, null))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isEqualTo(defaultStartDate)
    assertThat(taskSchedule.body!!.end).isNull()

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, CREATED, 1)
  }

  @Test
  fun `verify create task schedule with end`() {
    val taskSchedule =
        cut.createTaskSchedule(
            taskWithoutSchedule.identifier, CreateTaskScheduleResource(null, defaultEndDate))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isNull()
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultEndDate)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, CREATED, 1)
  }

  @Test
  fun `verify create multiple task schedules`() {
    val createTaskSchedules: MutableCollection<CreateTaskScheduleBatchResource> = ArrayList()
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(
            taskWithoutSchedule.identifier.toUuid(), defaultStartDate, defaultEndDate))
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(
            taskWithoutSchedule2.identifier.toUuid(),
            defaultShiftedStartDate,
            defaultShiftedEndDate))

    val taskSchedules = cut.createTaskSchedules(UpdateBatchRequestResource(createTaskSchedules))

    assertThat(taskSchedules).isNotNull
    assertThat(taskSchedules.body!!.taskSchedules).hasSize(2)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("start")
        .containsOnlyOnce(defaultStartDate)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("end")
        .containsOnlyOnce(defaultEndDate)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("start")
        .containsOnlyOnce(defaultShiftedStartDate)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("end")
        .containsOnlyOnce(defaultShiftedEndDate)

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)
    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, CREATED, 2, verifyNoOtherEventsExist = false)
  }

  @Test
  fun `verify create task schedule without start and end`() {
    assertThatThrownBy {
          cut.createTaskSchedule(
              taskWithoutSchedule.identifier, CreateTaskScheduleResource(null, null))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create task schedule with start after end`() {
    assertThatThrownBy {
          cut.createTaskSchedule(
              taskWithoutSchedule.identifier, CreateTaskScheduleResource(now(), now().minusDays(1)))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create task schedule fails if already exists`() {
    assertThatThrownBy {
          cut.createTaskSchedule(
              taskScheduleWithTwoDayCards.task!!.identifier,
              CreateTaskScheduleResource(defaultStartDate, defaultEndDate))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_ALREADY_EXISTS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple task schedules fails if one of them fails`() {
    val createTaskSchedules: MutableCollection<CreateTaskScheduleBatchResource> = ArrayList()
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(
            taskWithoutSchedule.identifier.toUuid(), defaultStartDate, defaultEndDate))
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(taskWithoutSchedule2.identifier.toUuid(), null, null))

    assertThatThrownBy { cut.createTaskSchedules(UpdateBatchRequestResource(createTaskSchedules)) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(PreconditionViolationException(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple task schedules fails if not from the same projects`() {
    val createTaskSchedules: MutableCollection<CreateTaskScheduleBatchResource> = ArrayList()
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(
            taskWithoutSchedule.identifier.toUuid(), defaultStartDate, defaultEndDate))
    createTaskSchedules.add(
        CreateTaskScheduleBatchResource(
            taskWithoutScheduleFromOtherProject.identifier.toUuid(),
            defaultStartDate,
            defaultEndDate))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createTaskSchedules(UpdateBatchRequestResource(createTaskSchedules)) }
        .withMessageKey(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create multiple task schedules fails for empty request`() {
    val createTaskSchedules: Collection<CreateTaskScheduleBatchResource> = ArrayList()

    assertThatThrownBy { cut.createTaskSchedules(UpdateBatchRequestResource(createTaskSchedules)) }
        .isInstanceOf(ConstraintViolationException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }
}
