/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify rfv customization state")
@SmartSiteSpringBootTest
class UpdateStateFromRfvCustomizationEventTest : AbstractIntegrationTest() {

  @Test
  fun `is saved after rfv customization created event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitRfvCustomization() }
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated after rfv customization update event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitRfvCustomization().submitRfvCustomization(eventType = UPDATED) {
        it.name = "Updated rfv customization"
      }
    }

    val rfvs = repositories.rfvCustomizationRepository.findAll()
    assertThat(rfvs).hasSize(1)
    assertThat(rfvs.first().identifier.identifier).isEqualTo(getIdentifier("rfvCustomization"))
    assertThat(rfvs.first().identifier.version).isEqualTo(1L)
    assertThat(rfvs.first().name).isEqualTo("Updated rfv customization")
  }
}
