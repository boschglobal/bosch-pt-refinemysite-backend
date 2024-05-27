/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a dayCard is created for a task assigned to a participant being a CR")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class DayCardCreatedWithTaskAssignedToCrNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCr() {
    eventStreamGenerator.submitTaskAsCr().submitTaskSchedule(auditUserReference = CR_USER)
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CR itself, the CSM is notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = CR_USER)
    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(1)

    checkNotificationForDayCardCreatedEventG2(
        notification = notifications.first(),
        requestUser = csmUser,
        actorUser = crUserAggregate,
        actorParticipant = crParticipantAggregate)
  }

  @Test
  fun `by the CSM, the CR (being the assignee) is notified`() {
    eventStreamGenerator.submitDayCardG2()
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForDayCardCreatedEventG2(
          notification = it.first(),
          requestUser = crUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `by somebody else, the CSM and the CR (being the assignee) are notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = FM_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }
    }
  }
}
