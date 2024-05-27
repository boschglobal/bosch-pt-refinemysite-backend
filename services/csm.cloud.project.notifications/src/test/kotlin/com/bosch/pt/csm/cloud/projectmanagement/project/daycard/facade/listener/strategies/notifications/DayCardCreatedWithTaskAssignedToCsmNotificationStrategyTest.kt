/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a day card is created for a task assigned to the CSM")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class DayCardCreatedWithTaskAssignedToCsmNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskWithScheduleAssignedToCsm() {
    eventStreamGenerator.submitTaskAsCsm().submitTaskSchedule()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CSM itself, nobody is notified`() {
    eventStreamGenerator.submitDayCardG2()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `by somebody else, the CSM is notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = FM_USER)
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForDayCardCreatedEventG2(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = fmUserAggregate,
          actorParticipant = fmParticipantAggregate)
    }
  }
}
