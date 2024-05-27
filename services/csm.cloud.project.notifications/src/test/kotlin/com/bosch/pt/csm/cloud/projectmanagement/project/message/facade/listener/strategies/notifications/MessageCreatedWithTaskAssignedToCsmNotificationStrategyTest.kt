/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to the CSM, is commented")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class MessageCreatedWithTaskAssignedToCsmNotificationStrategyTest :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCsm() {
    eventStreamGenerator.submitTaskAsCsm().submitTopicG2()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CSM itself, nobody is notified`() {
    eventStreamGenerator.submitMessage()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `by somebody else, the CSM is notified`() {
    eventStreamGenerator.submitMessage(auditUserReference = FM_USER)
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForCommentCreatedEvent(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = fmUserAggregate,
          actorParticipant = fmParticipantAggregate)
    }
  }
}
