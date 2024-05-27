/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromProjectEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.projectRepository.deleteAll()
  }

  @Test
  fun `is saved after project created event`() {
    assertThat(repositories.projectRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitProject() }
    assertThat(repositories.projectRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after project updated event`() {
    assertThat(repositories.projectRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitProject().submitProject(eventType = UPDATED) {
        it.title = "update project"
      }
    }

    val projects = repositories.projectRepository.findAll()
    assertThat(projects).hasSize(1)
    assertThat(projects.first().identifier.identifier).isEqualTo(getIdentifier("project"))
    assertThat(projects.first().identifier.version).isEqualTo(1L)
  }
}
