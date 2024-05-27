/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvListResource
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class RfvRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: RfvCustomizationAggregateAvro

  lateinit var aggregateV1: RfvCustomizationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query rfv with customization`() {
    submitEvents()

    // Execute query
    val rfvList = query(false)

    // Validate payload
    // 14 + 1 updated * 5 languages
    assertThat(rfvList.rfvs).hasSize((DayCardReasonEnum.values().size + 1) * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .filter { it.reason != DayCardReasonEnum.CHANGED_PRIORITY.key }
            .toMutableList()
            .apply {
              add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 0L))
              add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 1L))
            }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  @Test
  fun `query rfv with customization latest only`() {
    submitEvents()

    // Execute query
    val rfvList = query(true)

    // Validate payload
    // 14 * 5 languages
    assertThat(rfvList.rfvs).hasSize(DayCardReasonEnum.values().size * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .filter { it.reason != DayCardReasonEnum.CHANGED_PRIORITY.key }
            .toMutableList()
            .apply { add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 1L)) }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  @Test
  fun `query rfv without customization`() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    // Execute query
    val rfvList = query(false)

    // Validate payload
    // 14 * 5 languages
    assertThat(rfvList.rfvs).hasSize(DayCardReasonEnum.values().size * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  @Test
  fun `query rfv without customization latest only`() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    // Execute query
    val rfvList = query(true)

    // Validate payload
    // 14 * 5 languages
    assertThat(rfvList.rfvs).hasSize(DayCardReasonEnum.values().size * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  @Test
  fun `query deleted rfv customization`() {
    submitEvents()
    eventStreamGenerator.submitRfvCustomization(eventType = RfvCustomizationEventEnumAvro.DELETED)

    // Execute query
    val rfvList = query(false)

    // Validate payload
    // 14 + 1 updated + 1 deleted * 5 languages
    assertThat(rfvList.rfvs).hasSize((DayCardReasonEnum.values().size + 2) * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .filter { it.reason != DayCardReasonEnum.CHANGED_PRIORITY.key }
            .toMutableList()
            .apply {
              add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 0L))
              add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 1L))
              add(RfvRef(DayCardReasonEnum.CHANGED_PRIORITY.key, 2L))
            }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  @Test
  fun `query deleted rfv customization latest only`() {
    submitEvents()
    eventStreamGenerator.submitRfvCustomization(eventType = RfvCustomizationEventEnumAvro.DELETED)

    // Execute query
    val rfvList = query(true)

    // Validate payload
    // 14 * 5 languages
    assertThat(rfvList.rfvs).hasSize(DayCardReasonEnum.values().size * 5)

    val rfvs = rfvList.rfvs.map { RfvRef(it.reason, it.version) }.distinct()

    val expectedRfvs =
        DayCardReasonEnum.values()
            .map { RfvRef(it.key, -1L) }
            .sortedWith(compareBy({ it.reason }, { it.version }))

    assertThat(rfvs).isEqualTo(expectedRfvs)
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 =
        eventStreamGenerator
            .submitRfvCustomization {
              it.key = DayCardReasonNotDoneEnumAvro.CHANGED_PRIORITY
              it.name = "Important"
            }
            .get("rfvCustomization")!!

    aggregateV1 =
        eventStreamGenerator
            .submitRfvCustomization(eventType = RfvCustomizationEventEnumAvro.UPDATED) {
              it.name = "Very important"
            }
            .get("rfvCustomization")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/rfvs"), latestOnly, RfvListResource::class.java)

  data class RfvRef(val reason: String, val version: Long)
}
