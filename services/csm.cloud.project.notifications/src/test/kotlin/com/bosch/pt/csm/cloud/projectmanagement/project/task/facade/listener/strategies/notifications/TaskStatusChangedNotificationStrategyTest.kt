/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_STATUS_ENUM_STARTED
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * We only test one assignment scenario per status change since the recipient determination is
 * always the same and already tested with other types of notifications.
 */
@DisplayName("Notifications must be created")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskStatusChangedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun `when a task is open`() {
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `when a task is started`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.STARTED) {
      it.status = TaskStatusEnumAvro.STARTED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskStatusMessageKey = TASK_STATUS_ENUM_STARTED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskStatusMessageKey = TASK_STATUS_ENUM_STARTED,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `when a task is closed`() {
    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.STARTED) {
      it.status = TaskStatusEnumAvro.STARTED
    }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = FM_USER, eventType = TaskEventEnumAvro.CLOSED) {
      it.status = TaskStatusEnumAvro.CLOSED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_CLOSED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_CLOSED,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications when accept a draft task`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = EventStreamGeneratorStaticExtensions.getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.DRAFT
    }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ACCEPTED) {
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications when accept an open task`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = EventStreamGeneratorStaticExtensions.getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.OPEN
    }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ACCEPTED) {
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications when accept a started task`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = EventStreamGeneratorStaticExtensions.getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.STARTED
    }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ACCEPTED) {
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `verify notifications when accept a closed task`() {
    eventStreamGenerator
        .submitTask(auditUserReference = FM_USER) {
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference(FM_PARTICIPANT)
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask(auditUserReference = FM_USER, eventType = TaskEventEnumAvro.CLOSED) {
          it.status = TaskStatusEnumAvro.CLOSED
        }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ACCEPTED) {
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskStatusChangedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskStatusMessageKey = Key.TASK_STATUS_ENUM_ACCEPTED,
            taskAggregate = taskAggregate)
      }
    }
  }

  private fun requestAndCheckNotificationForTaskStatusChangedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      taskStatusMessageKey: String,
      taskAggregate: TaskAggregateAvro
  ) {
    val summary =
        NotificationSummaryDto(
            NOTIFICATION_SUMMARY_TASK_UPDATED,
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
        details = "Status \"${translate(taskStatusMessageKey)}\"")
  }
}
