/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromProjectCraftEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.projectCraftRepository.deleteAll()
    eventStreamGenerator.setUserContext("csm-user")
  }

  @Test
  fun `is saved after project craft created event`() {
    assertThat(repositories.projectCraftRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitProjectCraftG2() }
    assertThat(repositories.projectCraftRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after project craft updated event`() {
    assertThat(repositories.projectCraftRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitProjectCraftG2().submitProjectCraftG2(eventType = UPDATED) {
        it.name = "update craft"
      }
    }

    val crafts = repositories.projectCraftRepository.findAll()
    assertThat(crafts).hasSize(1)
    assertThat(crafts.first().identifier.identifier).isEqualTo(getIdentifier("projectCraft"))
    assertThat(crafts.first().identifier.version).isEqualTo(1L)
  }
}
