/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify task constraint customization state")
@SmartSiteSpringBootTest
class UpdateStateFromTaskConstraintCustomizationEventTest : AbstractIntegrationTest() {

  @Test
  fun `is updated after constraint customization created event`() {
    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitTaskConstraintCustomization() }
    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated after constraint customization update event`() {
    assertThat(repositories.taskConstraintCustomizationRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitTaskConstraintCustomization().submitTaskConstraintCustomization(
              eventType = UPDATED) { it.name = "Updated constraint customization" }
    }

    val constraints = repositories.taskConstraintCustomizationRepository.findAll()
    assertThat(constraints).hasSize(1)
    assertThat(constraints.first().identifier.identifier)
        .isEqualTo(getIdentifier("taskConstraintCustomization"))
    assertThat(constraints.first().identifier.version).isEqualTo(1L)
    assertThat(constraints.first().name).isEqualTo("Updated constraint customization")
  }
}
