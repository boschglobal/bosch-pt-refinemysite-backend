/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_START
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.randomString
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName(
    "When task schedule and task detail properties are changed" +
        "by the same participant within the configured look back time range")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskScheduleAndTaskUpdatedTest : BaseNotificationStrategyTest() {

  private val backToTheFutureInMillis = 499168800000

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule()

    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `the notifications are aggregated`() {
    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = backToTheFutureInMillis
        }
        .submitTask(eventType = TaskEventEnumAvro.UPDATED) { it.name = randomString() }

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${translate(TASK_SCHEDULE_ATTRIBUTE_START)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} " +
              "and ${translate(TASK_ATTRIBUTE_NAME)}"

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }
    }
  }

  @Test
  fun `the notifications are aggregated (multiple attribute changes)`() {
    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = backToTheFutureInMillis
          it.end = backToTheFutureInMillis
        }
        .submitTask(eventType = TaskEventEnumAvro.UPDATED) {
          it.name = randomString()
          it.location = randomString()
        }

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${translate(TASK_SCHEDULE_ATTRIBUTE_START)
            .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault())else it.toString() }}, " +
              "${translate(Key.TASK_SCHEDULE_ATTRIBUTE_END)}, " +
              "${translate(TASK_ATTRIBUTE_NAME)} and " +
              translate(Key.TASK_ATTRIBUTE_LOCATION)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskUpdatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            details = details,
            taskAggregate = taskAggregate)
      }
    }
  }
}
