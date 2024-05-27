/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class UpdateStateFromDayCardEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    repositories.dayCardRepository.deleteAll()
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
        }
        .setUserContext("csm-user")
  }

  @Test
  fun `is saved after dayCard created event`() {
    assertThat(repositories.dayCardRepository.findAll()).hasSize(0)
    eventStreamGenerator.repeat { eventStreamGenerator.submitDayCardG2() }
    assertThat(repositories.dayCardRepository.findAll()).hasSize(1)
  }

  @Test
  fun `is updated and cleaned up after dayCard updated event`() {
    assertThat(repositories.dayCardRepository.findAll()).hasSize(0)

    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitDayCardG2()
          .submitDayCardG2(eventType = UPDATED) { it.title = "update 1" }
          .submitDayCardG2(eventType = UPDATED) { it.title = "update 2" }
    }

    val dayCards = repositories.dayCardRepository.findAll()
    assertThat(dayCards).hasSize(2)
    assertThat(dayCards)
        .extracting("identifier")
        .extracting("identifier")
        .containsOnly(getIdentifier("dayCard"))
    assertThat(dayCards).extracting("identifier").extracting("version").containsAll(listOf(1L, 2L))
  }
}
