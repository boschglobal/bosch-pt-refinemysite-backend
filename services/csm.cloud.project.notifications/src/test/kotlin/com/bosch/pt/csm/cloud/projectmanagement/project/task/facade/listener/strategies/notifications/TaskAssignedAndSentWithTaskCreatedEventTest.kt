/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a task is created (CREATE event)")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAssignedAndSentWithTaskCreatedEventTest : BaseNotificationStrategyTest() {

  @Test
  fun `and the status is set to OPEN, task assignment notifications should be sent`() {
    eventStreamGenerator.submitTaskAsFm()

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            assigneeUser = fmUserAggregate,
            assigneeParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            assigneeUser = fmUserAggregate,
            assigneeParticipant = fmParticipantAggregate)
      }
    }
  }
}
