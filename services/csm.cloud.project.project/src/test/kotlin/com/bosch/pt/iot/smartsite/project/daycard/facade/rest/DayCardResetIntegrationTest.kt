/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedUpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.DONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.NOTDONE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
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
class DayCardResetIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: DayCardController

  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }
  private val dayCard2task1 by lazy { getIdentifier("dayCard2task1").asDayCardId() }
  private val dayCard3task1 by lazy { getIdentifier("dayCard3task1").asDayCardId() }
  private val dayCard1task2 by lazy { getIdentifier("dayCard1task2").asDayCardId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitInitProjectData()
        .submitTask { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule()
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task1") { it.status = DONE }
        .submitDayCardG2(asReference = "dayCard3task1") { it.status = NOTDONE }
        .submitTask(asReference = "task2") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "schedule2")
        .submitDayCardG2(asReference = "dayCard1task2") { it.status = APPROVED }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `resetting the state to 'OPEN'` {

    @Nested
    inner class `is successful` {

      @Test
      fun `from 'DONE' to 'OPEN'`() {
        val response = cut.resetDayCard(dayCard2task1, ETag.from(0))

        assertThat(response.body!!.status).isEqualTo(DayCardStatusEnum.OPEN)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, RESET, 1, true)
      }

      @Test
      fun `from 'NOT DONE' to 'OPEN'`() {
        val response = cut.resetDayCard(dayCard3task1, ETag.from(0))

        assertThat(response.body!!.status).isEqualTo(DayCardStatusEnum.OPEN)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, RESET, 1, true)
      }

      @Test
      fun `for multiple day cards`() {
        val response =
            cut.resetMultipleDayCards(
                VersionedUpdateBatchRequestResource(
                    setOf(
                        VersionedIdentifier(dayCard2task1.toUuid(), 0),
                        VersionedIdentifier(dayCard3task1.toUuid(), 0))),
                DAYCARD)

        assertThat(response.body!!.items.map { it.status }).containsOnly(DayCardStatusEnum.OPEN)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, RESET, 2, false)
      }

      @Test
      fun `for multiple day cards from different tasks are reset at once`() {
        val response =
            cut.resetMultipleDayCards(
                VersionedUpdateBatchRequestResource(
                    setOf(
                        VersionedIdentifier(dayCard2task1.toUuid(), 0),
                        VersionedIdentifier(dayCard1task2.toUuid(), 0))),
                DAYCARD)

        assertThat(response.body!!.items.size).isEqualTo(2)
        assertThat(response.body!!.items.map { it.status }).containsOnly(DayCardStatusEnum.OPEN)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, RESET, 2, false)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `if day card is reset that does not exist`() {
        val response =
            catchThrowableOfType(
                { cut.resetDayCard(DayCardId(), ETag.from(0)) }, AccessDeniedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card is reset that is already in status 'OPEN'`() {
        val response =
            catchThrowableOfType(
                { cut.resetDayCard(dayCard1task1, ETag.from(0)) },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are reset at once where one does not exist`() {
        val response =
            catchThrowableOfType(
                {
                  cut.resetMultipleDayCards(
                      VersionedUpdateBatchRequestResource(
                          setOf(
                              VersionedIdentifier(dayCard2task1.toUuid(), 0),
                              VersionedIdentifier(randomUUID(), 0))),
                      DAYCARD)
                },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are reset at once where one has a wrong status`() {
        val response =
            catchThrowableOfType(
                {
                  cut.resetMultipleDayCards(
                      VersionedUpdateBatchRequestResource(
                          setOf(
                              VersionedIdentifier(dayCard1task1.toUuid(), 0),
                              VersionedIdentifier(dayCard2task1.toUuid(), 0))),
                      DAYCARD)
                },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are approved at once with unsupported identifier type`() {
        val response =
            catchThrowableOfType(
                {
                  cut.resetMultipleDayCards(
                      VersionedUpdateBatchRequestResource(
                          setOf(
                              VersionedIdentifier(dayCard2task1.toUuid(), 0),
                              VersionedIdentifier(dayCard3task1.toUuid(), 0))),
                      TASK)
                },
                BatchIdentifierTypeNotSupportedException::class.java)

        assertThat(response.messageKey)
            .isEqualTo(COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }
    }
  }
}
