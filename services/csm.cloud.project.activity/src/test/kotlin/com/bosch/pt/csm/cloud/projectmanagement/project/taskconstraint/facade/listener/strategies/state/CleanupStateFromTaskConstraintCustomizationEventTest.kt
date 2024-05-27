/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify task constraint customization state")
@SmartSiteSpringBootTest
class CleanupStateFromTaskConstraintCustomizationEventTest : AbstractIntegrationTest() {

  @Test
  fun `is updated after constraint customization delete event`() {
    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskConstraintCustomization()
          .submitTaskConstraintCustomization(eventType = DELETED)
    }

    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(0)
  }
}
