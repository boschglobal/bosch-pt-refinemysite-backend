/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_ATTACHMENTS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTRIBUTE_WORK_AREA
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import java.time.Instant
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@DisplayName("When attachments are added to a task following a task update event")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAttachmentCreatedTogetherWithTaskUpdatedTest : BaseNotificationStrategyTest() {

  private val taskAttachment2Aggregate by lazy {
    context["secondAttachment"] as TaskAttachmentAggregateAvro
  }
  private val taskAttachment3Aggregate by lazy {
    context["thirdAttachment"] as TaskAttachmentAggregateAvro
  }

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator
        .setLastIdentifierForType(
            ProjectmanagementAggregateTypeEnum.PROJECTCRAFT.value, getByReference(PROJECT_CRAFT_1))
        .setLastIdentifierForType(
            ProjectmanagementAggregateTypeEnum.WORKAREA.value, getByReference(WORK_AREA_1))
        .submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `with a single change within less then 1 second by the same participant, notifications are aggregated`() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTask(eventType = TaskEventEnumAvro.UPDATED, time = time) {
          it.craft = getByReference(PROJECT_CRAFT_2)
        }
        .submitTaskAttachment(asReference = "firstAttachment", time = time.plusMillis(10))
        .submitTaskAttachment(asReference = "secondAttachment", time = time.plusMillis(20))
        .submitTaskAttachment(asReference = "thirdAttachment", time = time.plusMillis(30))

    assertThat(repositories.notificationRepository.findAll()).hasSize(8)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      val details =
          translate(TASK_ATTRIBUTE_CRAFT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
          } +
              " " +
              translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
              " " +
              translate(TASK_ATTRIBUTE_ATTACHMENTS)

      assertThat(notifications).hasSize(2)
      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment3Aggregate,
            details = details)
      }
      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment3Aggregate,
            details = details)
      }
    }
  }

  @Test
  fun `with multiple changes within less then 1 second by the same participant, notifications are aggregated`() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTask(eventType = TaskEventEnumAvro.UPDATED, time = time) {
          it.craft = getByReference(PROJECT_CRAFT_2)
          it.workarea = getByReference(WORK_AREA_2)
        }
        .submitTaskAttachment(asReference = "firstAttachment", time = time.plusMillis(10))
        .submitTaskAttachment(asReference = "secondAttachment", time = time.plusMillis(20))

    assertThat(repositories.notificationRepository.findAll()).hasSize(6)
    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      val details =
          translate(TASK_ATTRIBUTE_CRAFT).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
          } +
              ", " +
              translate(TASK_ATTRIBUTE_WORK_AREA) +
              " " +
              translate(MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR) +
              " " +
              translate(TASK_ATTRIBUTE_ATTACHMENTS)

      assertThat(notifications).hasSize(2)
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
