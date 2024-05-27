/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_RESET
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskResetNotificationStrategyTest : BaseNotificationStrategyTest() {

  @Test
  fun `verify notifications when reset a started task`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.STARTED
    }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.RESET) {
      it.status = TaskStatusEnumAvro.OPEN
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForResetTaskStatusEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForResetTaskStatusEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications when reset a closed task`() {
    eventStreamGenerator
        .submitTask(auditUserReference = FM_USER) {
          it.assignee = getByReference(FM_PARTICIPANT)
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask(auditUserReference = FM_USER, eventType = TaskEventEnumAvro.CLOSED) {
          it.status = TaskStatusEnumAvro.CLOSED
        }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.RESET) {
      it.status = TaskStatusEnumAvro.OPEN
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForResetTaskStatusEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForResetTaskStatusEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications are empty when there are no recipients`() {
    eventStreamGenerator
        .submitTask { it.status = TaskStatusEnumAvro.STARTED }
        .submitTask(eventType = TaskEventEnumAvro.CLOSED) { it.status = TaskStatusEnumAvro.CLOSED }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.RESET) {
      it.status = TaskStatusEnumAvro.OPEN
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(0)
    }
  }

  private fun requestAndCheckNotificationForResetTaskStatusEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      taskAggregate: TaskAggregateAvro
  ) {
    val summary =
        NotificationSummaryDto(
            NOTIFICATION_SUMMARY_TASK_RESET,
            mapOf(
                "originator" to
                    PlaceholderValueDto(
                        type = "PARTICIPANT",
                        id = actorParticipant.getIdentifier(),
                        text = "${actorUser.getFirstName()} ${actorUser.getLastName()}")))

    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = taskAggregate.buildObjectReference(),
        summary = summary,
        details = "Status \"Open\"")
  }
}
