/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class MessageCreatedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @Test
  fun `Notifications are generated without details if they do not contain text`() {
    eventStreamGenerator.submitTaskAsCr().submitTopicG2(auditUserReference = CR_USER)
    repositories.notificationRepository.deleteAll()

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
  fun `Notifications are not generated for deactivated participants`() {
    eventStreamGenerator
        .submitTaskAsCr()
        .submitTopicG2(auditUserReference = CR_USER)
        .submitParticipantG3(
            asReference = CR_PARTICIPANT, eventType = ParticipantEventEnumAvro.DEACTIVATED)
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitMessage()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }
}
