/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

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
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
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
    "When a task schedule has been updated and task attachment(s) have been created" +
        "by the same user withing the configured look back time range")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskScheduleChangedAndAttachmentCreatedTest : BaseNotificationStrategyTest() {

  private val taskAttachment1Aggregate by lazy {
    context["firstAttachment"] as TaskAttachmentAggregateAvro
  }

  private val taskAttachment2Aggregate by lazy {
    context["secondAttachment"] as TaskAttachmentAggregateAvro
  }

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm().submitTaskSchedule()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `the notifications are aggregated (for single attachment)`() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED, time = time) {
          it.start = taskScheduleStartDateInMillis
        }
        .submitTaskAttachment(asReference = "firstAttachment", time = time.plusMillis(10))

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      val details =
          "${
            translate(TASK_SCHEDULE_ATTRIBUTE_START)
              .replaceFirstChar {if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
          } and ${
            translate(TASK_ATTRIBUTE_ATTACHMENT)}"

      assertThat(notifications).hasSize(2)
      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment1Aggregate,
            details = details)
      }
      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment1Aggregate,
            details = details)
      }
    }
  }

  @Test
  fun `the notifications are aggregated (for two attachments)`() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED, time = time) {
          it.start = taskScheduleStartDateInMillis
          it.end = taskScheduleEndDateInMillis
        }
        .submitTaskAttachment(asReference = "firstAttachment", time = time.plusMillis(10))
        .submitTaskAttachment(asReference = "secondAttachment", time = time.plusMillis(20))

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      val details =
          "${
            translate(TASK_SCHEDULE_ATTRIBUTE_START)
              .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}, ${
            translate(TASK_SCHEDULE_ATTRIBUTE_END)} and ${
            translate(TASK_ATTRIBUTE_ATTACHMENTS)}"

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details = details)
      }
      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details = details)
      }
    }
  }
}
