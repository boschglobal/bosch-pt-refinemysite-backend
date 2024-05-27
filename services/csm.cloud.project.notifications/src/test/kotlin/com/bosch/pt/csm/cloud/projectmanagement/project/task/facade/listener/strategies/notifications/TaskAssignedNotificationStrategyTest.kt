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
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAssignedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @Test
  fun `Notifications are not generated for task create event with status draft`() {
    eventStreamGenerator.submitTask {
      it.status = TaskStatusEnumAvro.DRAFT
      it.assignee = getByReference(FM_PARTICIPANT)
    }

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `Notifications are not generated for task assigned event with status draft`() {
    eventStreamGenerator
        .submitTask {
          it.status = TaskStatusEnumAvro.DRAFT
          it.assignee = getByReference(FM_PARTICIPANT)
        }
        .submitTask(eventType = TaskEventEnumAvro.ASSIGNED)

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `Notifications are not generated for task update event with status draft`() {
    eventStreamGenerator
        .submitTask {
          it.status = TaskStatusEnumAvro.DRAFT
          it.assignee = getByReference(FM_PARTICIPANT)
        }
        .submitTask(eventType = TaskEventEnumAvro.UPDATED)

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `Notifications are not generated for task sent event without assignee`() {
    eventStreamGenerator
        .submitTask { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTask(eventType = TaskEventEnumAvro.SENT)

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }
}
