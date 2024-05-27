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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class CleanupStateFromDayCardDeletedEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitTask(auditUserReference = "fm-user") {
          it.assignee = getByReference("fm-participant")
        }
        .setUserContext("csm-user")
        .submitDayCardG2()
  }

  @Test
  fun `is cleaned up after dayCard deleted event`() {
    assertThat(findDayCard()).isNotNull
    eventStreamGenerator.submitDayCardG2(eventType = DELETED)
    assertThat(findDayCard()).isNull()
  }

  private fun findDayCard() =
      repositories.dayCardRepository.findLatest(getIdentifier("dayCard"), project.getIdentifier())
}
