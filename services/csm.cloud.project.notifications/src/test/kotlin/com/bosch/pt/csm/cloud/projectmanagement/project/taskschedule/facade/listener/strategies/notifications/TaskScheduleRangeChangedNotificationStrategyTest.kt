/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("Notifications must be created on task schedule change")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskScheduleRangeChangedNotificationStrategyTest : BaseNotificationStrategyTest() {

  private val scheduleAggregate by lazy { context["taskSchedule"] as TaskScheduleAggregateAvro }

  private val backToTheFutureInMillis = 499168800000

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `when a single attribute has been changed`() {
    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.start = backToTheFutureInMillis
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date \"26 Oct 1985\"",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date \"26 Oct 1985\"",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }
    }
  }

  @Test
  fun `when a single attribute has been removed`() {
    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.start = taskScheduleStartDateInMillis
      it.end = taskScheduleEndDateInMillis
    }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.end = null
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "End date removed",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "End date removed",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }
    }
  }

  @Test
  fun `when multiple attributes have been changed`() {
    eventStreamGenerator.submitTaskSchedule(
        auditUserReference = FM_USER, eventType = TaskScheduleEventEnumAvro.UPDATED) {
      it.start = taskScheduleStartDateInMillis
      it.end = null
      it.slots = emptyList()
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date and end date",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate,
            details = "Start date and end date",
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }
    }
  }
}
