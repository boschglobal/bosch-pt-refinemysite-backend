/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.FRIDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.MONDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.SATURDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.THURSDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.TUESDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro.WEDNESDAY
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_DATE
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.SaveDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.UpdateDayCardResource
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleSlotResource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@Suppress("ClassName")
@EnableAllKafkaListeners
class DayCardIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: DayCardController

  private val defaultStartDate = now()
  private val defaultEndDate = defaultStartDate.plusDays(10)

  private val task1 by lazy { getIdentifier("task1").asTaskId() }
  private val task3 by lazy { getIdentifier("task3").asTaskId() }
  private val task4 by lazy { getIdentifier("task4").asTaskId() }

  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }
  private val dayCard2task1 by lazy { getIdentifier("dayCard2task1").asDayCardId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task1") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "schedule") {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
        }
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task1") { it.status = DONE }
        .submitTaskSchedule(asReference = "schedule", eventType = UPDATED) {
          it.slots =
              listOf(
                  getByReference("dayCard1task1").asSlot(now()),
                  getByReference("dayCard2task1").asSlot(now().plusDays(1)))
        }
        .submitTask(asReference = "task2") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "schedule2") {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
        }
        .submitDayCardG2 { it.status = OPEN }
        .submitTask(asReference = "task3") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "scheduleWithoutStartDate") { it.start = null }
        .submitTask(asReference = "task4") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "scheduleWithoutEndDate") { it.end = null }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `adding a day card to a task schedule` {

    @Nested
    inner class `is successful` {

      @Test
      fun `at a free slot`() {
        val response =
            cut.addDayCardToTaskSchedule(
                    task1,
                    null,
                    SaveDayCardResource(
                        "Great Title",
                        BigDecimal.valueOf(1),
                        "My big notes",
                        defaultStartDate.plusDays(3)),
                    ETag.from(1))
                .body!!

        assertSlotsAreOrdered(response)
        projectEventStoreUtils.verifyContainsInSequence(
            listOf(DayCardEventG2Avro::class.java, TaskScheduleEventAvro::class.java))
        projectEventStoreUtils.verifyContains(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.CREATED, 1, false)
        projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
      }

      @Test
      fun `at an occupied slot`() {
        val dateToFree = LocalDate.of(2023, 3, 3)

        eventStreamGenerator
            .submitWorkdayConfiguration(
                asReference = "workdayConfiguration",
                eventType = WorkdayConfigurationEventEnumAvro.UPDATED) {
                  it.startOfWeek = MONDAY
                  it.workingDays = mutableListOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                  it.allowWorkOnNonWorkingDays = false
                }
            .submitTask(asReference = "anotherTask") {
              it.assignee = getByReference("participantCsm1")
            }
            .submitTaskSchedule(asReference = "anotherSchedule") {
              it.start = dateToFree.toEpochMilli()
              it.end = dateToFree.plusDays(21).toEpochMilli()
            }
            .submitDayCardG2(asReference = "dayCard1anotherTask") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard2anotherTask") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard3anotherTask") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard4anotherTask") { it.status = OPEN }
            .submitTaskSchedule(asReference = "anotherSchedule", eventType = UPDATED) {
              it.slots =
                  listOf(
                      getByReference("dayCard1anotherTask").asSlot(dateToFree),
                      getByReference("dayCard2anotherTask").asSlot(dateToFree.plusDays(2)),
                      getByReference("dayCard3anotherTask").asSlot(dateToFree.plusDays(3)),
                      getByReference("dayCard4anotherTask").asSlot(dateToFree.plusDays(8)))
            }

        val response =
            cut.addDayCardToTaskSchedule(
                    getIdentifier("anotherTask").asTaskId(),
                    null,
                    SaveDayCardResource(
                        "Great Title", BigDecimal.valueOf(1), "My big notes", dateToFree),
                    ETag.from(1))
                .body!!

        assertThat(response.slots).hasSize(5)
        assertThat(response.slots!!).anyMatch { it.date == dateToFree }
        assertThat(response.slots!!.associate { it.dayCard.identifier to it.date })
            .containsAllEntriesOf(
                mapOf(
                    getIdentifier("dayCard1anotherTask") to dateToFree.plusDays(3),
                    getIdentifier("dayCard2anotherTask") to dateToFree.plusDays(4),
                    getIdentifier("dayCard3anotherTask") to dateToFree.plusDays(5),
                    getIdentifier("dayCard4anotherTask") to dateToFree.plusDays(8)))

        projectEventStoreUtils.verifyContainsInSequence(
            listOf(DayCardEventG2Avro::class.java, TaskScheduleEventAvro::class.java))
        projectEventStoreUtils.verifyContains(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.CREATED, 1, false)
        projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
      }
    }

    @Nested
    inner class `reports an error` {

      @Test
      fun `when all slots are occupied`() {
        val taskWithAllOccupiedSlotsIdentifier = randomString()
        eventStreamGenerator
            .submitTask(asReference = "taskScheduleWithAllOccupiedSlots") {
              it.aggregateIdentifierBuilder.identifier = taskWithAllOccupiedSlotsIdentifier
              it.assignee = getByReference("participantCsm1")
            }
            .submitTaskSchedule(asReference = "scheduleWithOccupiedSlots") {
              it.start = defaultStartDate.toEpochMilli()
              it.end = defaultStartDate.plusDays(1).toEpochMilli()
            }
            .submitDayCardG2(asReference = "dayCard1") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard2") { it.status = OPEN }
            .submitTaskSchedule(asReference = "scheduleWithOccupiedSlots", eventType = UPDATED) {
              it.slots =
                  listOf(
                      getByReference("dayCard1").asSlot(now()),
                      getByReference("dayCard2").asSlot(now().plusDays(1)))
            }
        projectEventStoreUtils.reset()

        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      taskWithAllOccupiedSlotsIdentifier.asTaskId(),
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(1))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey)
            .isEqualTo(TASK_SCHEDULE_VALIDATION_ERROR_POSITION_OUT_OF_RANGE)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `when all slots are occupied twice`() {
        val taskWithAllOccupiedSlotsIdentifier = randomString()
        eventStreamGenerator
            .submitTask(asReference = "taskScheduleWithAllOccupiedSlots") {
              it.aggregateIdentifierBuilder.identifier = taskWithAllOccupiedSlotsIdentifier
              it.assignee = getByReference("participantCsm1")
            }
            .submitTaskSchedule(asReference = "scheduleWithOccupiedSlots") {
              it.start = defaultStartDate.toEpochMilli()
              it.end = defaultStartDate.plusDays(1).toEpochMilli()
            }
            .submitDayCardG2(asReference = "dayCard1") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard2") { it.status = OPEN }
            .submitDayCardG2(asReference = "dayCard3") { it.status = OPEN }
            .submitTaskSchedule(asReference = "scheduleWithOccupiedSlots", eventType = UPDATED) {
              it.slots =
                  listOf(
                      getByReference("dayCard1").asSlot(now()),
                      getByReference("dayCard2").asSlot(now()),
                      getByReference("dayCard3").asSlot(now()))
            }

        projectEventStoreUtils.reset()

        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      taskWithAllOccupiedSlotsIdentifier.asTaskId(),
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(1))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey)
            .isEqualTo(TASK_SCHEDULE_VALIDATION_ERROR_MULTIPLE_DAY_CARD_AT_SAME_POSITION)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `when slot to be moved contains non-open day card`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task1,
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(1))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `when slot is from a non-working day`() {
        eventStreamGenerator.submitWorkdayConfiguration(
            asReference = "workdayConfiguration",
            eventType = WorkdayConfigurationEventEnumAvro.UPDATED) {
              it.startOfWeek = MONDAY
              it.workingDays = mutableListOf()
              it.allowWorkOnNonWorkingDays = false
            }

        assertThatExceptionOfType(PreconditionViolationException::class.java)
            .isThrownBy {
              cut.addDayCardToTaskSchedule(
                  task1,
                  null,
                  SaveDayCardResource(
                      "Great Title",
                      BigDecimal.valueOf(1),
                      "My big notes",
                      defaultStartDate.plusDays(3)),
                  ETag.from(1))
            }
            .withMessageContaining(DAY_CARD_VALIDATION_ERROR_DATE)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `when the working days does not contains the day of the week`() {
        eventStreamGenerator.submitWorkdayConfiguration(
            asReference = "workdayConfiguration",
            eventType = WorkdayConfigurationEventEnumAvro.UPDATED) {
              it.startOfWeek = SATURDAY
              it.workingDays = mutableListOf()
              it.allowWorkOnNonWorkingDays = false
            }

        assertThatExceptionOfType(PreconditionViolationException::class.java)
            .isThrownBy {
              cut.addDayCardToTaskSchedule(
                  task1,
                  null,
                  SaveDayCardResource(
                      "Great Title",
                      BigDecimal.valueOf(1),
                      "My big notes",
                      defaultStartDate.plusDays(3)),
                  ETag.from(1))
            }
            .withMessageContaining(DAY_CARD_VALIDATION_ERROR_DATE)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for not having a task schedule`() {
        val taskId = TaskId()
        eventStreamGenerator.submitTask {
          it.aggregateIdentifierBuilder.identifier = taskId.toString()
        }

        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      taskId,
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(1))
                },
                AggregateNotFoundException::class.java)

        assertThat(response.messageKey).isEqualTo(TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for an outdated ETag`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task1,
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(0))
                },
                EntityOutdatedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for task schedule having no start date`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task3,
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(0))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for task schedule having no end date`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task4,
                      null,
                      SaveDayCardResource(
                          "Great Title", BigDecimal.valueOf(1), "My big notes", defaultStartDate),
                      ETag.from(0))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(TASK_SCHEDULE_VALIDATION_ERROR_DATE_TIMES)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for date of day card being before start date of task schedule`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task1,
                      null,
                      SaveDayCardResource(
                          "Great Title",
                          BigDecimal.valueOf(1),
                          "My big notes",
                          defaultStartDate.minusDays(1)),
                      ETag.from(1))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_DATE_TIMES)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for date of day card being after end date of task schedule`() {
        val response =
            catchThrowableOfType(
                {
                  cut.addDayCardToTaskSchedule(
                      task1,
                      null,
                      SaveDayCardResource(
                          "Great Title",
                          BigDecimal.valueOf(1),
                          "My big notes",
                          defaultEndDate.plusDays(1)),
                      ETag.from(1))
                },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_DATE_TIMES)
        projectEventStoreUtils.verifyEmpty()
      }
    }
  }

  @Nested
  inner class `updating a day card` {

    @Nested
    inner class `is successful` {

      @Test
      fun `with valid parameters`() {
        val updateDayCardResource =
            UpdateDayCardResource("Updated title", BigDecimal.valueOf(73.50), "Updated notes")

        val response = cut.updateDayCard(dayCard1task1, updateDayCardResource, ETag.from(0)).body!!

        assertThat(response.title).isEqualTo("Updated title")
        assertThat(response.manpower).isEqualTo("73.50")
        assertThat(response.notes).isEqualTo("Updated notes")
        projectEventStoreUtils.verifyContains(
            DayCardEventG2Avro::class.java, DayCardEventEnumAvro.UPDATED, 1, true)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `when day card is not open anymore`() {
        val updateDayCardResource =
            UpdateDayCardResource(randomString(), BigDecimal.valueOf(73.50), randomString())

        val response =
            catchThrowableOfType(
                { cut.updateDayCard(dayCard2task1, updateDayCardResource, ETag.from(0)) },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `when the day card does not exist`() {
        val updateDayCardResource =
            UpdateDayCardResource(randomString(), BigDecimal.valueOf(73.50), randomString())

        assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
          cut.updateDayCard(DayCardId(), updateDayCardResource, ETag.from(0))
        }

        projectEventStoreUtils.verifyEmpty()
      }
    }
  }

  private fun assertSlotsAreOrdered(resource: TaskScheduleResource) {
    val taskSlotsIterator: Iterator<TaskScheduleSlotResource> = resource.slots!!.iterator()
    var currentSlotDate: LocalDate? = null

    while (taskSlotsIterator.hasNext()) {
      val nextSlot = taskSlotsIterator.next()
      if (currentSlotDate == null) {
        currentSlotDate = nextSlot.date
      } else {
        assertThat(currentSlotDate).isBefore(nextSlot.date)
      }
    }
  }
}
