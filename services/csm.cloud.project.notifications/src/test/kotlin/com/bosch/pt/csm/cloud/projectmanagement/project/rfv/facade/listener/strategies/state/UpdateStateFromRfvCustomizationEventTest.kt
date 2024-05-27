/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [RestDocumentationExtension::class, SpringExtension::class])
@DisplayName("State must")
@SmartSiteSpringBootTest
class UpdateStateFromRfvCustomizationEventTest : BaseNotificationTest() {

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `be updated for rfv customization created event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitRfvCustomization() }

    val rfvs = repositories.rfvCustomizationRepository.findAll()
    assertThat(rfvs).hasSize(1)

    validateAttributes(rfvs.first(), get("rfvCustomization")!!)
  }

  @Test
  fun `be updated for rfv customization updated event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitRfvCustomization().submitRfvCustomization(eventType = UPDATED) {
        it.name = "Updated rfv customization"
      }
    }

    val rfvs = repositories.rfvCustomizationRepository.findAll()
    assertThat(rfvs).hasSize(2)
    assertThat(rfvs.map { it.identifier.identifier }.toSet()).hasSize(1)
    assertThat(rfvs.map { it.identifier.version }.toSet()).isEqualTo(setOf(0L, 1L))

    val latest = rfvs.first { it.identifier.version == 1L }
    validateAttributes(latest, get("rfvCustomization")!!)
  }

  private fun validateAttributes(
      rfvCustomization: RfvCustomization,
      aggregate: RfvCustomizationAggregateAvro
  ) {
    assertThat(rfvCustomization.name).isEqualTo(aggregate.getName())
    assertThat(rfvCustomization.reason)
        .isEqualTo(DayCardReasonEnum.valueOf(aggregate.getKey().name))
    assertThat(rfvCustomization.active).isEqualTo(aggregate.getActive())
    assertThat(rfvCustomization.identifier)
        .isEqualTo(aggregate.getAggregateIdentifier().toAggregateIdentifier())
    assertThat(rfvCustomization.projectIdentifier).isEqualTo(aggregate.getProjectIdentifier())
  }
}
