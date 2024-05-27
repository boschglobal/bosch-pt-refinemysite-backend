/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify day card state")
@SmartSiteSpringBootTest
class CleanupStateFromDayCardDeletedEventTest : BaseNotificationStrategyTest() {

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator
        .submitTaskAsFm()
        .submitTaskSchedule(auditUserReference = FM_USER)
        .submitDayCardG2(auditUserReference = FM_USER)
  }

  @Test
  fun `is cleaned up from daycard deleted event`() {
    var dayCards = repositories.dayCardRepository.findDayCards(projectAggregate.getIdentifier())
    assertThat(dayCards).hasSize(1)

    eventStreamGenerator.repeat { eventStreamGenerator.submitDayCardG2(eventType = DELETED) }

    dayCards = repositories.dayCardRepository.findDayCards(projectAggregate.getIdentifier())
    assertThat(dayCards).isEmpty()
  }
}
