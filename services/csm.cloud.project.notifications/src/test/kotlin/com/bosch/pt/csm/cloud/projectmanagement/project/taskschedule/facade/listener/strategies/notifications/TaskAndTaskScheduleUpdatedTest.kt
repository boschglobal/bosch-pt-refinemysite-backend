/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_START
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.randomString
import java.time.Instant
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName(
    "When task details and schedule properties are changed " +
        "by the same participant within the configured look back time range")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAndTaskScheduleUpdatedTest : BaseNotificationStrategyTest() {

  private val scheduleAggregate by lazy { context["taskSchedule"] as TaskScheduleAggregateAvro }

  private val backToTheFutureInMillis = 499168800000

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule()
    repositories.notificationRepository.deleteAll()
  }

  @DisplayName("the notifications are aggregated")
  @Test
  fun aggregateTaskUpdatedAndTaskScheduleUpdated() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTask(eventType = TaskEventEnumAvro.UPDATED, time = time) { it.name = randomString() }
        .submitTaskSchedule(
            eventType = TaskScheduleEventEnumAvro.UPDATED, time = time.plusMillis(10)) {
          it.start = backToTheFutureInMillis
        }

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${translate(TASK_SCHEDULE_ATTRIBUTE_START)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} " +
              "and ${translate(TASK_ATTRIBUTE_NAME)}"

      notifications.selectFirstFor(crUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }
    }
  }

  @DisplayName("and containing multiple attribute changes, the notifications are aggregated")
  @Test
  fun aggregateTaskUpdatedAndTaskScheduleUpdatedWithMultipleAttributeChanges() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTask(eventType = TaskEventEnumAvro.UPDATED, time = time) {
          it.name = randomString()
          it.location = randomString()
        }
        .submitTaskSchedule(
            eventType = TaskScheduleEventEnumAvro.UPDATED, time = time.plusMillis(10)) {
          it.start = backToTheFutureInMillis
          it.end = backToTheFutureInMillis
        }

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${translate(TASK_SCHEDULE_ATTRIBUTE_START)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, " +
              "${translate(TASK_SCHEDULE_ATTRIBUTE_END)}, " +
              "${translate(TASK_ATTRIBUTE_NAME)} and " +
              translate(TASK_ATTRIBUTE_LOCATION)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForScheduleUpdatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate,
            scheduleAggregate = scheduleAggregate)
      }
    }
  }
}
