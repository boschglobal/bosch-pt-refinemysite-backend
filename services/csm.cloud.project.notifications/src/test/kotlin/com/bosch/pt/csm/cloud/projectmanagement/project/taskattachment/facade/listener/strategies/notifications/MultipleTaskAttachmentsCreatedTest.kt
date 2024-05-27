/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import java.time.Instant
import org.apache.commons.text.StringSubstitutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * We only test one assignment scenario since the recipient determination is always the same and
 * already tested with other types of notifications.
 */
@DisplayName("When multiple attachments are added to a task ")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class MultipleTaskAttachmentsCreatedTest : BaseNotificationStrategyTest() {

  private val taskAttachment1Aggregate by lazy {
    context["firstAttachment"] as TaskAttachmentAggregateAvro
  }
  private val taskAttachment2Aggregate by lazy {
    context["secondAttachment"] as TaskAttachmentAggregateAvro
  }
  private val taskAttachment3Aggregate by lazy {
    context["thirdAttachment"] as TaskAttachmentAggregateAvro
  }

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `within less then 1 second by the same participant, the notifications are aggregated`() {
    val now = Instant.now()
    eventStreamGenerator
        .submitTaskAttachment(asReference = "firstAttachment", time = now)
        .submitTaskAttachment(asReference = "secondAttachment", time = now.plusMillis(10))

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details =
                StringSubstitutor(mapOf("number" to "2"), "\${", "}")
                    .replace(translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_MULTIPLE)))
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details =
                StringSubstitutor(mapOf("number" to "2"), "\${", "}")
                    .replace(translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_MULTIPLE)))
      }
    }
  }

  @Test
  fun `within less then 1 second by the same participant and mixed order, the notifications are aggregated`() {
    val time = Instant.now()
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitTaskAttachment(asReference = "firstAttachment", time = time)
          .submitTaskAttachment(asReference = "secondAttachment", time = time.plusMillis(10))
          .submitTaskAttachment(asReference = "fourthAttachment", time = time.plusMillis(960))
          .submitTaskAttachment(asReference = "thirdAttachment", time = time.plusMillis(970))
    }

    assertThat(repositories.notificationRepository.findAll()).hasSize(8)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment3Aggregate,
            details =
                StringSubstitutor(mapOf("number" to "4"), "\${", "}")
                    .replace(translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_MULTIPLE)))
      }
    }
  }

  @Test
  fun `within less then 1 second by different participants, the notifications are NOT aggregated`() {
    val time = Instant.now()
    eventStreamGenerator
        .submitTaskAttachment(
            auditUserReference = CSM_USER, asReference = "firstAttachment", time = time)
        .submitTaskAttachment(
            auditUserReference = CR_USER,
            asReference = "secondAttachment",
            time = time.plusMillis(10))

    assertThat(repositories.notificationRepository.findAll()).hasSize(4)

    repositories.notificationRepository.findAllByMergedFalse().also { notifications ->
      assertThat(notifications).hasSize(4)

      notifications.selectFirstFor(csmUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details = translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE))
      }

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment1Aggregate,
            details = translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE))
      }

      notifications.selectFirstFor(fmUser, taskAttachment1Aggregate.getAggregateIdentifier()).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            taskAttachmentAggregate = taskAttachment1Aggregate,
            details = translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE),
            expectedNumberOfResults = 2,
            indexOfNotificationToVerify = 1)
      }

      notifications.selectFirstFor(fmUser, taskAttachment2Aggregate.getAggregateIdentifier()).also {
        checkNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate,
            taskAttachmentAggregate = taskAttachment2Aggregate,
            details = translate(Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE),
            expectedNumberOfResults = 2,
            indexOfNotificationToVerify = 0)
      }
    }
  }
}
