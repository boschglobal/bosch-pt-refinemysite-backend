/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationFinishedEventAvro
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.MESSAGE
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OF_SAME_TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_OPEN
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.DeleteMultipleDayCardFromScheduleResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.DeleteMultipleDayCardResource
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import jakarta.validation.ConstraintViolationException
import java.time.LocalDate.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@Suppress("ClassName")
@EnableAllKafkaListeners
class DayCardDeleteIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: DayCardController

  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }
  private val dayCard2task1 by lazy { getIdentifier("dayCard2task1").asDayCardId() }
  private val dayCard1task2 by lazy { getIdentifier("dayCard1task2").asDayCardId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitInitProjectData()
        .submitTask(asReference = "task1") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule()
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task1") { it.status = DONE }
        .submitTaskSchedule(eventType = UPDATED) {
          it.slots =
              listOf(
                  getByReference("dayCard1task1").asSlot(now()),
                  getByReference("dayCard2task1").asSlot(now().plusDays(1)))
        }
        .submitTask(asReference = "task2") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "schedule2")
        .submitDayCardG2(asReference = "dayCard1task2") { it.status = OPEN }
        .submitTaskSchedule(asReference = "schedule2", eventType = UPDATED) {
          it.slots = listOf(getByReference("dayCard1task2").asSlot(now()))
        }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `deleting a day card` {

    @Nested
    inner class `is successful` {

      @Test
      fun `for valid parameters`() {
        val response = cut.deleteDayCardAndSlot(dayCard1task1, ETag.from(1))

        assertThat(response.body!!.slots).hasSize(1)
        projectEventStoreUtils.verifyContainsInSequence(
            listOf(TaskScheduleEventAvro::class.java, DayCardEventG2Avro::class.java))
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, DELETED, 1, false)
        projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 1, false)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `if the IF-MATCH header does not match`() {
        val response =
            catchThrowableOfType(
                { cut.deleteDayCardAndSlot(dayCard1task1, ETag.from(0)) },
                EntityOutdatedException::class.java)

        assertThat(response.messageKey).isEqualTo(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if the IF-MATCH header is missing`() {
        val response =
            catchThrowableOfType(
                { cut.deleteDayCardAndSlot(dayCard1task1, ETag.from(0)) },
                EntityOutdatedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card does not exist`() {
        val response =
            catchThrowableOfType(
                { cut.deleteDayCardAndSlot(DayCardId(), ETag.from(0)) },
                AccessDeniedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card is in a non-open state`() {
        val response =
            catchThrowableOfType(
                { cut.deleteDayCardAndSlot(dayCard2task1, ETag.from(1)) },
                PreconditionViolationException::class.java)

        assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
        projectEventStoreUtils.verifyEmpty()
      }
    }
  }

  @Nested
  inner class `deleting day cards` {

    @Nested
    inner class `is successful` {

      @Test
      fun `for valid parameters`() {
        val additionalDayCardIdentifier by lazy { getIdentifier("dayCard3task1") }

        eventStreamGenerator.submitDayCardG2("dayCard3task1") {
          it.task = getByReference("task1")
          it.status = OPEN
        }

        val response =
            cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                BatchRequestResource(
                    LinkedHashSet(listOf(dayCard1task1.toUuid(), additionalDayCardIdentifier))),
                DAYCARD,
                ETag.from(1))

        assertThat(response.body!!.slots).hasSize(1)
        projectEventStoreUtils.verifyContainsInSequence(
            listOf(
                BatchOperationStartedEventAvro::class.java,
                TaskScheduleEventAvro::class.java,
                DayCardEventG2Avro::class.java,
                TaskScheduleEventAvro::class.java,
                DayCardEventG2Avro::class.java,
                BatchOperationFinishedEventAvro::class.java))
        projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 2, false)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, DELETED, 2, false)
      }

      @Test
      fun `for valid parameters for different task schedules`() {
        val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
        val additionalDayCardIdentifier by lazy { getIdentifier("dayCard3task1").asDayCardId() }

        eventStreamGenerator.submitDayCardG2("dayCard3task1") {
          it.task = getByReference("task1")
          it.status = OPEN
        }

        val response =
            cut.deleteMultipleDayCardsAndSlots(
                DeleteMultipleDayCardResource(
                    listOf(
                        DeleteMultipleDayCardFromScheduleResource(
                            1L, setOf(dayCard1task1, additionalDayCardIdentifier)),
                        DeleteMultipleDayCardFromScheduleResource(1L, setOf(dayCard1task2)))),
                DAYCARD,
                projectIdentifier)

        assertThat(response.body!!.items).hasSize(2)
        projectEventStoreUtils.verifyContainsInSequence(
            listOf(
                BatchOperationStartedEventAvro::class.java,
                TaskScheduleEventAvro::class.java,
                DayCardEventG2Avro::class.java,
                TaskScheduleEventAvro::class.java,
                DayCardEventG2Avro::class.java,
                TaskScheduleEventAvro::class.java,
                DayCardEventG2Avro::class.java,
                BatchOperationFinishedEventAvro::class.java))
        projectEventStoreUtils.verifyContains(TaskScheduleEventAvro::class.java, UPDATED, 3, false)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, DELETED, 3, false)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `if the IF-MATCH header does not match`() {
        val response =
            catchThrowableOfType(
                {
                  cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                      BatchRequestResource(LinkedHashSet(listOf(dayCard1task1.toUuid()))),
                      DAYCARD,
                      ETag.from(0))
                },
                EntityOutdatedException::class.java)

        assertThat(response.messageKey).isEqualTo(COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
        projectEventStoreUtils.verifyEmpty()
      }
    }

    @Test
    fun `if the IF-MATCH header is missing`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(LinkedHashSet(listOf(dayCard1task1.toUuid()))),
                    DAYCARD,
                    ETag.from(0))
              },
              EntityOutdatedException::class.java)

      assertThat(response).isNotNull
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if one day card does not exist`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(LinkedHashSet(listOf(randomUUID()))),
                    DAYCARD,
                    ETag.from(0))
              },
              AccessDeniedException::class.java)

      assertThat(response).isNotNull
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if one day card is in a non-open state`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(LinkedHashSet(listOf(dayCard2task1.toUuid()))),
                    DAYCARD,
                    ETag.from(1))
              },
              PreconditionViolationException::class.java)

      assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_NOT_OPEN)
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if one day card does not exist in general`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(
                        LinkedHashSet(listOf(dayCard1task1.toUuid(), randomUUID()))),
                    DAYCARD,
                    ETag.from(1))
              },
              PreconditionViolationException::class.java)

      assertThat(response).isNotNull
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if day cards do not belong to the same task`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(
                        LinkedHashSet(listOf(dayCard1task1.toUuid(), dayCard1task2.toUuid()))),
                    DAYCARD,
                    ETag.from(1))
              },
              PreconditionViolationException::class.java)

      assertThat(response.messageKey).isEqualTo(DAY_CARD_VALIDATION_ERROR_NOT_OF_SAME_TASK)
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if no day card identifiers are given`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(emptySet()), DAYCARD, ETag.from(1))
              },
              ConstraintViolationException::class.java)

      assertThat(response).isNotNull
      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `if identifier type is not day card`() {
      val response =
          catchThrowableOfType(
              {
                cut.deleteMultipleDayCardsAndSlotsFromSchedule(
                    BatchRequestResource(LinkedHashSet(listOf(randomUUID()))),
                    MESSAGE,
                    ETag.from(0))
              },
              BatchIdentifierTypeNotSupportedException::class.java)

      assertThat(response.messageKey)
          .isEqualTo(COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      projectEventStoreUtils.verifyEmpty()
    }
  }
}
