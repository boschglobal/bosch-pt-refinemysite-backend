/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
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
class DayCardQueryIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: DayCardController

  private val dayCard1task1 by lazy { getIdentifier("dayCard1task1").asDayCardId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitInitProjectData()
        .submitTask { it.assignee = getByReference("participantCsm1") }
        .submitTaskSchedule()
        .submitDayCardG2(asReference = "dayCard1task1") { it.status = OPEN }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `finding a single day card` {

    @Nested
    inner class `is successful` {

      @Test
      fun `for valid parameters`() {
        val response = cut.findByDayCardIdentifier(dayCard1task1)

        assertThat(response.body).isNotNull

        projectEventStoreUtils.verifyEmpty()
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `for non-existing identifier`() {
        val response =
            catchThrowableOfType(
                { cut.findByDayCardIdentifier(DayCardId()) }, AccessDeniedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }
    }
  }

  @Nested
  inner class `finding multiple day cards` {

    @Nested
    inner class `is successful` {

      @Test
      fun `for valid parameters`() {
        val response =
            cut.findAllByDayCardIdentifiers(
                BatchRequestResource(setOf(dayCard1task1.toUuid())), DAYCARD)

        assertThat(response.body).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }

      @Test
      fun `for non-supported identifier type`() {
        val response =
            catchThrowableOfType(
                {
                  cut.findAllByDayCardIdentifiers(
                      BatchRequestResource(setOf(dayCard1task1.toUuid())), TASK)
                },
                BatchIdentifierTypeNotSupportedException::class.java)

        assertThat(response.messageKey)
            .isEqualTo(COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }
    }

    @Nested
    inner class `reports error` {

      @Test
      fun `for non-existing identifier`() {
        val response =
            catchThrowableOfType(
                {
                  cut.findAllByDayCardIdentifiers(
                      BatchRequestResource(setOf(randomUUID())), DAYCARD)
                },
                AccessDeniedException::class.java)

        assertThat(response).isNotNull
        projectEventStoreUtils.verifyEmpty()
      }
    }
  }
}
