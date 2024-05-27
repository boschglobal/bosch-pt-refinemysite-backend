/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENTS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ATTRIBUTE_START
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
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
    "When a task attachment has been created and the task schedule has been updated " +
        "by the same participant within the configured look back time range")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAttachmentCreatedAndScheduleUpdatedTest : BaseNotificationStrategyTest() {

  private val scheduleAggregate by lazy { context["taskSchedule"] as TaskScheduleAggregateAvro }

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule()
    repositories.notificationRepository.deleteAll()
  }

  @DisplayName("the notifications are aggregated (for single attachment)")
  @Test
  fun aggregateEventsForTaskAttachmentAndTaskSchedule() {
    val time = Instant.now()
    eventStreamGenerator.submitTaskAttachment(time = time).submitTaskSchedule(
        eventType = TaskScheduleEventEnumAvro.UPDATED, time = time.plusMillis(10)) {
      it.start = taskScheduleStartDateInMillis
    }

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${
            translate(TASK_SCHEDULE_ATTRIBUTE_START)
              .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} and ${
            translate(TASK_ATTRIBUTE_ATTACHMENT)}"

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

  @DisplayName("the notifications are aggregated (for two attachments)")
  @Test
  fun severalAttachmentsSeveralScheduleChanges() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTaskAttachment(asReference = "taskAttachment1", time = time)
        .submitTaskAttachment(asReference = "taskAttachment2", time = time.plusMillis(10))
        .submitTaskSchedule(
            eventType = TaskScheduleEventEnumAvro.UPDATED, time = time.plusMillis(20)) {
          it.start = taskScheduleStartDateInMillis
          it.end = taskScheduleEndDateInMillis
        }

    assertThat(repositories.notificationRepository.findAll()).hasSize(6)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${
            translate(TASK_SCHEDULE_ATTRIBUTE_START)
              .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, ${
            translate(TASK_SCHEDULE_ATTRIBUTE_END)} and ${
            translate(TASK_ATTRIBUTE_ATTACHMENTS)}"

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
