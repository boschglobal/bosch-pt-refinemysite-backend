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
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCsm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a topic of a task, assigned to the CSM, is created")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TopicCreatedWithTaskAssignedToCsmNotificationStrategyG2Test :
    BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCsm() {
    eventStreamGenerator.submitTaskAsCsm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CSM itself, nobody is notified`() {
    eventStreamGenerator.submitTopicG2()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `by somebody else, the CSM is notified`() {
    eventStreamGenerator.submitTopicG2(auditUserReference = FM_USER)

    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForTopicCreatedEventG2(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = fmUserAggregate,
          actorParticipant = fmParticipantAggregate)
    }
  }
}
