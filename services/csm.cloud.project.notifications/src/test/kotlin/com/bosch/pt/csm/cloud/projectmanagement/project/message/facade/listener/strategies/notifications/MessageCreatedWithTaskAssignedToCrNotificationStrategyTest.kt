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
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to a participant being a CR, is commented")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class MessageCreatedWithTaskAssignedToCrNotificationStrategyTest :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCr() {
    eventStreamGenerator.submitTaskAsCr().submitTopicG2(auditUserReference = "cr-user")
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CR itself, the CSM is notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = CR_USER)
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForCommentCreatedEvent(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = crUserAggregate,
          actorParticipant = crParticipantAggregate)
    }
  }

  @Test
  fun `by the CSM, the CR (being the assignee) is notified`() {
    eventStreamGenerator.submitMessage()
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForCommentCreatedEvent(
          notification = it.first(),
          requestUser = crUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `by somebody else, the CSM and the CR (being the assignee) are notified`() {
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
}
