/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to a participant being an FM, is created")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TopicCreatedWithTaskAssignedToFmNotificationStrategyG2Test :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToFm() {
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the FM (being the assignee) itself, the CSM and the CR are notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = FM_USER)

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTopicCreatedEventG2(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTopicCreatedEventG2(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the CR of the assigned FM, the CSM and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = CR_USER)

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }
    val notificationForAssignee =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = crUserAggregate,
        actorParticipant = crParticipantAggregate)

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForAssignee,
        requestUser = fmUser,
        actorUser = crUserAggregate,
        actorParticipant = crParticipantAggregate)
  }

  @Test
  fun `by the CSM, the CR of the assignee and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitTopicG2()

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }
    val notificationForAssignee =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForAssignee,
        requestUser = fmUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)
  }

  @Test
  fun `by another participant, the FM (being the assignee), the CR (of the assignee) and the CSM are notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = OTHER_FM_USER)

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(3)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }
    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }
    val notificationForAssignee =
        notifications.first { it.notificationIdentifier.recipientIdentifier == fmUser.identifier }

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = otherFmUserAggregate,
        actorParticipant = otherFmParticipantAggregate)

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = otherFmUserAggregate,
        actorParticipant = otherFmParticipantAggregate)

    // Check notification of FM (being the assignee)
    checkNotificationForTopicCreatedEventG2(
        notification = notificationForAssignee,
        requestUser = fmUser,
        actorUser = otherFmUserAggregate,
        actorParticipant = otherFmParticipantAggregate)
  }
}
