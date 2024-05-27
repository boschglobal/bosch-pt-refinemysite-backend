/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("Notifications must be created on task schedule delete events")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskScheduleRemovedNotificationStrategyTest : BaseNotificationStrategyTest() {

  private val scheduleAggregate by lazy { context["taskSchedule"] as TaskScheduleAggregateAvro }

  @Test
  fun `when previous task schedule had start and end date`() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule(auditUserReference = FM_USER)
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.DELETED)

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationsForScheduleEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date and end date",
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationsForScheduleEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date and end date",
            scheduleAggregate = scheduleAggregate)
      }
    }
  }

  @Test
  fun `when previous task schedule had only start date`() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule(auditUserReference = FM_USER) {
      it.end = null
    }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.DELETED)

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationsForScheduleEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date removed",
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationsForScheduleEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date removed",
            scheduleAggregate = scheduleAggregate)
      }
    }
  }

  private fun checkNotificationsForScheduleEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro,
      details: String,
      scheduleAggregate: TaskScheduleAggregateAvro
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
        objectReference = scheduleAggregate.buildObjectReference(),
        summary = summary,
        details = details)
  }
}
