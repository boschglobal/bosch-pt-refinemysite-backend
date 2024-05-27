/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromWorkAreaEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.workAreaRepository.deleteAll()
  }

  @Test
  fun `is saved after work area created event`() {
    assertThat(repositories.workAreaRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitWorkArea() }
    assertThat(repositories.workAreaRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after work area updated event`() {
    assertThat(repositories.workAreaRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitWorkArea().submitWorkArea(eventType = UPDATED) {
        it.name = "update workArea"
      }
    }

    val workAreas = repositories.workAreaRepository.findAll()
    assertThat(workAreas).hasSize(1)
    assertThat(workAreas.first().identifier.identifier).isEqualTo(getIdentifier("workArea"))
    assertThat(workAreas.first().identifier.version).isEqualTo(1L)
  }
}
