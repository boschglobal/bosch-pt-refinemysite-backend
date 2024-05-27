/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_DAY_CARD_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_SLOTS_NOT_MATCH
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleBatchResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.UpdateTaskScheduleResource
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
class UpdateTaskScheduleIntegrationTest : AbstractTaskScheduleIntegrationTest() {

  @Test
  fun `verify update task schedule with start and end`() {
    val taskSchedule =
        cut.updateTaskSchedule(
            taskScheduleWithoutDayCard.task.identifier,
            UpdateTaskScheduleResource(defaultShiftedStartDate, defaultShiftedEndDate, emptyList()),
            ETag.from(taskScheduleWithoutDayCard.version.toString()))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isEqualTo(defaultShiftedStartDate)
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultShiftedEndDate)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1)
  }

  @Test
  fun `verify update task schedule with start`() {
    val taskSchedule =
        cut.updateTaskSchedule(
            taskScheduleWithoutDayCard.task.identifier,
            UpdateTaskScheduleResource(defaultShiftedStartDate, null, emptyList()),
            ETag.from(taskScheduleWithoutDayCard.version.toString()))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isEqualTo(defaultShiftedStartDate)
    assertThat(taskSchedule.body!!.end).isNull()

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1)
  }

  @Test
  fun `verify update task schedule with end`() {
    val taskSchedule =
        cut.updateTaskSchedule(
            taskScheduleWithoutDayCard.task.identifier,
            UpdateTaskScheduleResource(null, defaultShiftedEndDate, emptyList()),
            ETag.from(taskScheduleWithoutDayCard.version.toString()))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isNull()
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultShiftedEndDate)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1)
  }

  @Test
  fun `verify update task schedule without slots`() {
    val taskSchedule =
        cut.updateTaskSchedule(
            taskScheduleWithoutDayCard.task.identifier,
            UpdateTaskScheduleResource(null, defaultShiftedEndDate, null),
            ETag.from(taskScheduleWithoutDayCard.version.toString()))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isNull()
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultShiftedEndDate)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1)
  }

  @Test
  fun `verify update task schedule with slots`() {
    val taskSchedule =
        cut.updateTaskSchedule(
            taskScheduleWithTwoDayCards.task.identifier,
            UpdateTaskScheduleResource(
                defaultShiftedStartDate,
                defaultShiftedEndDate,
                shiftSlots(taskScheduleWithTwoDayCards, defaultShift)),
            ETag.from(taskScheduleWithTwoDayCards.version.toString()))

    assertThat(taskSchedule).isNotNull
    assertThat(taskSchedule.body!!.start).isEqualTo(defaultShiftedStartDate)
    assertThat(taskSchedule.body!!.end).isEqualTo(defaultShiftedEndDate)
    assertSlotsAreOrdered(
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            taskScheduleWithTwoDayCards.task.identifier)!!)

    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1)
  }

  @Test
  fun `verify update multiple task schedules`() {
    val updateTaskSchedules: MutableCollection<UpdateTaskScheduleBatchResource> = ArrayList()
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskScheduleWithTwoDayCards.version,
            taskScheduleWithTwoDayCards.task.identifier.toUuid(),
            defaultStartDate.plusDays(1),
            defaultEndDate.plusDays(1),
            shiftSlots(taskScheduleWithTwoDayCards, defaultShift)))
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskScheduleWithoutDayCard.version,
            taskScheduleWithoutDayCard.task.identifier.toUuid(),
            defaultShiftedStartDate,
            defaultShiftedEndDate,
            emptyList()))

    val taskSchedules = cut.updateTaskSchedules(UpdateBatchRequestResource(updateTaskSchedules))

    assertThat(taskSchedules).isNotNull
    assertThat(taskSchedules.body!!.taskSchedules).hasSize(2)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("start")
        .containsOnlyOnce(defaultStartDate.plusDays(1))
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("end")
        .containsOnlyOnce(defaultEndDate.plusDays(1))
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("start")
        .containsOnlyOnce(defaultShiftedStartDate)
    assertThat(taskSchedules.body!!.taskSchedules)
        .extracting("end")
        .containsOnlyOnce(defaultShiftedEndDate)
    assertSlotsAreOrdered(
        taskScheduleRepository.findWithDetailsByTaskIdentifier(
            taskScheduleWithTwoDayCards.task.identifier)!!)

    projectEventStoreUtils.verifyContainsInSequence(
        BatchOperationStartedEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        TaskScheduleEventAvro::class.java,
        BatchOperationFinishedEventAvro::class.java)
    projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 2, false)
  }

  @Test
  fun `verify update task schedule without start and end`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithoutDayCard.task.identifier,
              UpdateTaskScheduleResource(null, null, emptyList()),
              ETag.from(taskScheduleWithoutDayCard.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with start after end`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithoutDayCard.task.identifier,
              UpdateTaskScheduleResource(now(), now().minusDays(1), emptyList()),
              ETag.from(taskScheduleWithoutDayCard.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with task schedule not found`() {

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              TaskId(),
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate, defaultShiftedEndDate, emptyList()),
              ETag.from("1"))
        }
        .withMessageKey(TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with outdated etag`() {

    assertThatExceptionOfType(EntityOutdatedException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithoutDayCard.task.identifier,
              UpdateTaskScheduleResource(now(), now().minusDays(1), emptyList()),
              ETag.from((taskScheduleWithoutDayCard.version - 1).toString()))
        }
        .withMessageKey(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slots and without end`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate,
                  null,
                  shiftSlots(taskScheduleWithTwoDayCards, defaultShift)),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slots and without start`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  null,
                  defaultShiftedEndDate,
                  shiftSlots(taskScheduleWithTwoDayCards, defaultShift)),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with invalid slot size`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate,
                  defaultShiftedEndDate,
                  removeSlot(taskScheduleWithTwoDayCards, dayCard2Identifier)),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_SLOTS_NOT_MATCH)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with no slot`() {

    assertThatExceptionOfType(NullPointerException::class.java).isThrownBy {
      cut.updateTaskSchedule(
          taskScheduleWithTwoDayCards.task.identifier,
          UpdateTaskScheduleResource(
              defaultShiftedStartDate, defaultShiftedEndDate, removeSlot(null, dayCard2Identifier)),
          ETag.from(taskScheduleWithTwoDayCards.version.toString()))
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slot with new day card`() {
    eventStreamGenerator.submitDayCardG2("newDayCard") {
      it.task = taskScheduleWithTwoDayCards.task.identifier.toAggregateReference()
      it.status = OPEN
    }

    val dayCardIdentifier = getIdentifier("newDayCard").asDayCardId()

    projectEventStoreUtils.reset()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskIdentifier = taskScheduleWithTwoDayCards.task.identifier,
              resource =
                  UpdateTaskScheduleResource(
                      start = defaultShiftedStartDate,
                      end = defaultShiftedEndDate,
                      slots =
                          changeIdentifierOfSlot(
                              taskSchedule = taskScheduleWithTwoDayCards,
                              oldIdentifier = dayCard2Identifier,
                              newIdentifier = dayCardIdentifier)),
              eTag = ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_EXISTING_DAY_CARD_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slot with same position`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  defaultStartDate,
                  defaultEndDate,
                  changeDateOfSlot(
                      taskScheduleWithTwoDayCards, dayCard2Identifier, defaultStartDate)),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slot with position higher than duration`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate,
                  defaultShiftedEndDate,
                  changeDateOfSlot(
                      taskScheduleWithTwoDayCards, dayCard2Identifier, defaultEndDate.plusDays(1))),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slot with position lower than duration`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithTwoDayCards.task.identifier,
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate,
                  defaultShiftedEndDate,
                  changeDateOfSlot(
                      taskScheduleWithTwoDayCards,
                      dayCard2Identifier,
                      defaultStartDate.minusDays(1))),
              ETag.from(taskScheduleWithTwoDayCards.version.toString()))
        }
        .withMessage(TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task schedule with slot of not open day card changed position`() {

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          cut.updateTaskSchedule(
              taskScheduleWithNonOpenDayCard.task.identifier,
              UpdateTaskScheduleResource(
                  defaultShiftedStartDate,
                  defaultShiftedEndDate,
                  changeDateOfSlot(
                      taskScheduleWithNonOpenDayCard,
                      dayCardApprovedIdentifier,
                      defaultStartDate.plusDays(2))),
              ETag.from(taskScheduleWithNonOpenDayCard.version.toString()))
        }
        .withMessage(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update multiple task schedules fails if one of them fails`() {
    val updateTaskSchedules: MutableCollection<UpdateTaskScheduleBatchResource> = ArrayList()
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskScheduleWithTwoDayCards.version,
            taskScheduleWithTwoDayCards.task.identifier.toUuid(),
            defaultStartDate.plusDays(1),
            defaultEndDate.plusDays(1),
            shiftSlots(taskScheduleWithTwoDayCards, defaultShift)))
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskScheduleWithoutDayCard.version + 1,
            taskScheduleWithoutDayCard.task.identifier.toUuid(),
            defaultShiftedStartDate,
            defaultShiftedEndDate,
            emptyList()))

    assertThatExceptionOfType(EntityOutdatedException::class.java)
        .isThrownBy { cut.updateTaskSchedules(UpdateBatchRequestResource(updateTaskSchedules)) }
        .withMessageKey(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update multiple task schedules fails if from different projects`() {
    val updateTaskSchedules: MutableCollection<UpdateTaskScheduleBatchResource> = ArrayList()
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskScheduleWithTwoDayCards.version,
            taskScheduleWithTwoDayCards.task.identifier.toUuid(),
            defaultStartDate.plusDays(1),
            defaultEndDate.plusDays(1),
            shiftSlots(taskScheduleWithTwoDayCards, defaultShift)))
    updateTaskSchedules.add(
        UpdateTaskScheduleBatchResource(
            taskWithScheduleFromOtherProject.taskSchedule!!.version,
            taskWithScheduleFromOtherProject.identifier.toUuid(),
            defaultShiftedStartDate,
            defaultShiftedEndDate,
            emptyList()))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTaskSchedules(UpdateBatchRequestResource(updateTaskSchedules)) }
        .withMessage(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)

    projectEventStoreUtils.verifyEmpty()
  }
}
