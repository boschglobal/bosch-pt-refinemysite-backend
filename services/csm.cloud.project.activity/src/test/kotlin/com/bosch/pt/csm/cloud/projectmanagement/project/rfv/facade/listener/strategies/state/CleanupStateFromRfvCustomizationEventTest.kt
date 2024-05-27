/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify rfv customization state")
@SmartSiteSpringBootTest
class CleanupStateFromRfvCustomizationEventTest : AbstractIntegrationTest() {

  @Test
  fun `is updated after rfv customization delete event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitRfvCustomization().submitRfvCustomization(eventType = DELETED)
    }

    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
  }
}
