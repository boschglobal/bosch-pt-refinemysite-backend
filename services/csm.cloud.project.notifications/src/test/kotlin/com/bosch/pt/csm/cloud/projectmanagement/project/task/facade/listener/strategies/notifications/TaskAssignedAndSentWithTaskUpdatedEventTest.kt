/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When a task is updated (UPDATE event)")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAssignedAndSentWithTaskUpdatedEventTest : BaseNotificationStrategyTest() {

  @Test
  fun `and the status is set from DRAFT to OPEN, task assignment notifications should be sent`() {
    eventStreamGenerator
        .submitTask {
          it.assignee = null
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask(eventType = TaskEventEnumAvro.UPDATED) {
          it.assignee = getByReference(FM_PARTICIPANT)
          it.status = TaskStatusEnumAvro.OPEN
          it.workarea = getByReference(WORK_AREA_2)
        }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            assigneeUser = fmUserAggregate,
            assigneeParticipant = fmParticipantAggregate)
      }
    }
  }
}
