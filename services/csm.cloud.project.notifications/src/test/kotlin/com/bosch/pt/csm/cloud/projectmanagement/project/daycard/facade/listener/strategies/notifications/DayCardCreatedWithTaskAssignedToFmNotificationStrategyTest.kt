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
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a day card is created for a task assigned to a participant being an FM")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class DayCardCreatedWithTaskAssignedToFmNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToFm() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule(auditUserReference = FM_USER)
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the FM (being the assignee) itself, the CSM and the CR are notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = FM_USER)
    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }
    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    checkNotificationForDayCardCreatedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    checkNotificationForDayCardCreatedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }

  @Test
  fun `by the CR of the assigned FM, the CSM and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = CR_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the CSM, the CR of the assignee and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitDayCardG2()
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }

  @Test
  fun `by another participant, the FM (being the assignee), the CR (of the assignee) and the CSM are notified`() {
    eventStreamGenerator.submitDayCardG2(auditUserReference = OTHER_FM_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(3)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForDayCardCreatedEventG2(
            notification = it,
            requestUser = fmUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }
    }
  }
}
