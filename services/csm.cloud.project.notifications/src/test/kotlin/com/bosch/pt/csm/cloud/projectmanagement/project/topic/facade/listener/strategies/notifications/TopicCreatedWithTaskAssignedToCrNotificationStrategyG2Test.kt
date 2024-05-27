/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to a participant being a CR, is created")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TopicCreatedWithTaskAssignedToCrNotificationStrategyG2Test :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCr() {
    eventStreamGenerator.submitTaskAsCr()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CR itself, the CSM is notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = CR_USER)

    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)
      checkNotificationForTopicCreatedEventG2(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = crUserAggregate,
          actorParticipant = crParticipantAggregate)
    }
  }

  @Test
  fun `by the CSM, the CR (being the assignee) is notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = CSM_USER)

    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForTopicCreatedEventG2(
          notification = it.first(),
          requestUser = crUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `by somebody else, the CSM and the CR (being the assignee) are notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = FM_USER)

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).hasSize(2)

    val notificationForCsm =
        notifications.first { it.notificationIdentifier.recipientIdentifier == csmUser.identifier }

    val notificationForCr =
        notifications.first { it.notificationIdentifier.recipientIdentifier == crUser.identifier }

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCsm,
        requestUser = csmUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)

    checkNotificationForTopicCreatedEventG2(
        notification = notificationForCr,
        requestUser = crUser,
        actorUser = fmUserAggregate,
        actorParticipant = fmParticipantAggregate)
  }
}
