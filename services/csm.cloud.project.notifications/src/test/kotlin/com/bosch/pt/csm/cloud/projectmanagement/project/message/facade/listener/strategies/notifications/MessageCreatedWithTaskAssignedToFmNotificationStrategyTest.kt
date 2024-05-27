/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to a participant being an FM, is commented")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class
MessageCreatedWithTaskAssignedToFmNotificationStrategyTest :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCr() {
    eventStreamGenerator.submitTaskAsFm().submitTopicG2(auditUserReference = "fm-user")
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the FM (being the assignee) itself, the CSM and the CR are notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = FM_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the CR of the assigned FM, the CSM and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = CR_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the CSM, the CR of the assignee and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = "csm-user")
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }

  @Test
  fun `by another participant, the FM (being the assignee), the CR (of the assignee) and the CSM are notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = OTHER_FM_USER)
    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(3)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForCommentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = otherFmUserAggregate,
            actorParticipant = otherFmParticipantAggregate)
      }
    }
  }
}
