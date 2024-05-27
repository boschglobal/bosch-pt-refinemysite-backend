/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
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
import com.bosch.pt.iot.smartsite.common.i18n.Key
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelMultipleDayCardsResource
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CUSTOM1
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.UpdateRfvDto
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ClassName")
@EnableAllKafkaListeners
class DayCardCancellationIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: DayCardController

  @Autowired private lateinit var rfvService: RfvService

  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }
  private val dayCard2task1 by lazy { getIdentifier("dayCard2task1").asDayCardId() }
  private val dayCard3task1 by lazy { getIdentifier("dayCard3task1").asDayCardId() }
  private val dayCard4task1 by lazy { getIdentifier("dayCard4task1").asDayCardId() }
  private val dayCard5task1 by lazy { getIdentifier("dayCard5task1").asDayCardId() }
  private val dayCard1task2 by lazy { getIdentifier("dayCard1task2").asDayCardId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitInitProjectData()
        .submitTask { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule()
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard2task1") { it.status = APPROVED }
        .submitDayCardG2(asReference = "dayCard3task1") { it.status = OPEN }
        .submitDayCardG2(asReference = "dayCard4task1") { it.status = NOTDONE }
        .submitDayCardG2(asReference = "dayCard5task1") { it.status = DONE }
        .submitTask(asReference = "task2") { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule(asReference = "schedule2")
        .submitDayCardG2(asReference = "dayCard1task2") { it.status = OPEN }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `changing the state to canceled` {

    @Nested
    inner class `is successful` {

      @Test
      fun `from 'OPEN' to 'NOT DONE'`() {
        val cancelDayCardResource = CancelDayCardResource(BAD_WEATHER)

        val response = cut.cancelDayCard(dayCard1task1, cancelDayCardResource, ETag.from(0))

        assertThat(response.body!!.status).isEqualTo(DayCardStatusEnum.NOTDONE)
        assertThat(response.body!!.reason!!.key).isEqualTo(BAD_WEATHER)
        assertThat(response.body!!.reason!!.name).isEqualTo("Weather")
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, CANCELLED, 1, true)
      }

      @Test
      fun `from 'DONE to 'NOT DONE'`() {
        val cancelDayCardResource = CancelDayCardResource(BAD_WEATHER)

        val response = cut.cancelDayCard(dayCard5task1, cancelDayCardResource, ETag.from(0))

        assertThat(response.body!!.status).isEqualTo(DayCardStatusEnum.NOTDONE)
        assertThat(response.body!!.reason!!.key).isEqualTo(BAD_WEATHER)
        assertThat(response.body!!.reason!!.name).isEqualTo("Weather")
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, CANCELLED, 1, true)
      }

      @Test
      fun `for multiple day cards`() {
        val cancelMultipleDayCardsResource =
            CancelMultipleDayCardsResource(
                setOf(
                    VersionedIdentifier(dayCard1task1.toUuid(), 0),
                    VersionedIdentifier(dayCard3task1.toUuid(), 0)),
                BAD_WEATHER)

        val response = cut.cancelMultipleDayCards(cancelMultipleDayCardsResource, DAYCARD)

        assertThat(response.body!!.items.map { it.status }).containsOnly(DayCardStatusEnum.NOTDONE)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, CANCELLED, 2, false)
      }

      @Test
      fun `for multiple day cards from different tasks are canceled at once`() {
        val cancelMultipleDayCardsResource =
            CancelMultipleDayCardsResource(
                setOf(
                    VersionedIdentifier(dayCard1task1.toUuid(), 0),
                    VersionedIdentifier(dayCard1task2.toUuid(), 0)),
                BAD_WEATHER)

        val response = cut.cancelMultipleDayCards(cancelMultipleDayCardsResource, DAYCARD)

        assertThat(response.body!!.items.size).isEqualTo(2)
        assertThat(response.body!!.items.map { it.status }).containsOnly(DayCardStatusEnum.NOTDONE)
        projectEventStoreUtils.verifyContains(DayCardEventG2Avro::class.java, CANCELLED, 2, false)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `if day card is canceled that does not exist`() {
        val cancelDayCardResource = CancelDayCardResource(BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelDayCard(DayCardId(), cancelDayCardResource, ETag.from(0)) },
                AggregateNotFoundException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card is canceled that is already in status 'NOT DONE'`() {
        val cancelDayCardResource = CancelDayCardResource(BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelDayCard(dayCard4task1, cancelDayCardResource, ETag.from(0)) },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card is canceled that is in non-'OPEN' state`() {
        val cancelDayCardResource = CancelDayCardResource(BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelDayCard(dayCard2task1, cancelDayCardResource, ETag.from(0)) },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if day card is canceled with a deactivated reason`() {
        // Deactivate the reason
        rfvService.update(
            UpdateRfvDto(getIdentifier("project").asProjectId(), CUSTOM1, false, null))
        projectEventStoreUtils.reset()

        // Try to use the deactivated reason
        val cancelDayCardResource = CancelDayCardResource(CUSTOM1)

        val throwable = catchThrowable {
          cut.cancelDayCard(dayCard1task1, cancelDayCardResource, ETag.from(0))
        }

        assertThat(throwable).isInstanceOf(PreconditionViolationException::class.java)
        assertThat((throwable as PreconditionViolationException).messageKey)
            .isEqualTo(Key.RFV_VALIDATION_ERROR_REASON_DEACTIVATED)
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are canceled at once where one does not exist`() {
        val cancelMultipleDayCardsResource =
            CancelMultipleDayCardsResource(
                setOf(
                    VersionedIdentifier(dayCard1task1.toUuid(), 0),
                    VersionedIdentifier(randomUUID(), 0)),
                BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelMultipleDayCards(cancelMultipleDayCardsResource, DAYCARD) },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are canceled at once where one has a wrong status`() {
        val cancelMultipleDayCardsResource =
            CancelMultipleDayCardsResource(
                setOf(
                    VersionedIdentifier(dayCard1task1.toUuid(), 0),
                    VersionedIdentifier(dayCard2task1.toUuid(), 0)),
                BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelMultipleDayCards(cancelMultipleDayCardsResource, DAYCARD) },
                PreconditionViolationException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `if multiple day cards are canceled at once with unsupported identifier type`() {
        val cancelMultipleDayCardsResource =
            CancelMultipleDayCardsResource(
                setOf(
                    VersionedIdentifier(dayCard1task1.toUuid(), 0),
                    VersionedIdentifier(dayCard3task1.toUuid(), 0)),
                BAD_WEATHER)

        val response =
            catchThrowableOfType(
                { cut.cancelMultipleDayCards(cancelMultipleDayCardsResource, TASK) },
                BatchIdentifierTypeNotSupportedException::class.java)

        assertThat(response.messageKey)
            .isEqualTo(COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }
    }
  }
}
