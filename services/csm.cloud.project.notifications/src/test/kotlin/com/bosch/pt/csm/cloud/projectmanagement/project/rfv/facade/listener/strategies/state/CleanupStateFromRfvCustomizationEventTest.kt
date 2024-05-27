/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
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
class CleanupStateFromRfvCustomizationEventTest : BaseNotificationTest() {

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `be cleaned up for rfv customization updated event`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitRfvCustomization().submitRfvCustomization(eventType = DELETED)
    }

    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
  }
}
