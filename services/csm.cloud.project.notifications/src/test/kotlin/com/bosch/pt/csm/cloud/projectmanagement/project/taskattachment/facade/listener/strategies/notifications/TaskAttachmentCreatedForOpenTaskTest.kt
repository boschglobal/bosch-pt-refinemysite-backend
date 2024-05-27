/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.NOTIFICATION_SUMMARY_TASK_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.NotificationSummaryGenerator.buildSummary
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
 * We only test one assignment scenario since the recipient determination is always the same and
 * already tested with other types of notifications.
 */
@DisplayName("When an attachment was added to a task ")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAttachmentCreatedForOpenTaskTest : BaseNotificationStrategyTest() {

  private val taskAttachmentAggregate by lazy {
    context["taskAttachment"] as TaskAttachmentAggregateAvro
  }

  @BeforeEach
  fun createOpenTask() {
    eventStreamGenerator.submitTaskAsFm()
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `by the CSM, the CR and assigned FM are notified`() {
    eventStreamGenerator.submitTaskAttachment()

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the CR, the CSM and assigned FM are notified`() {
    eventStreamGenerator.submitTaskAttachment(auditUserReference = CR_USER)

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = crUserAggregate,
            actorParticipant = crParticipantAggregate)
      }
    }
  }

  @Test
  fun `by the assigned FM, the CSM and CR are notified`() {
    eventStreamGenerator.submitTaskAttachment(auditUserReference = FM_USER)

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(csmUser).also {
        requestAndCheckNotificationForTaskAttachmentCreatedEvent(
            notification = it,
            requestUser = csmUser,
            actorUser = fmUserAggregate,
            actorParticipant = fmParticipantAggregate)
      }
    }
  }

  private fun requestAndCheckNotificationForTaskAttachmentCreatedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = taskAttachmentAggregate.buildObjectReference(),
        summary = buildSummary(NOTIFICATION_SUMMARY_TASK_UPDATED, actorParticipant, actorUser),
        details = translate(NOTIFICATION_DETAILS_TASK_ATTACHMENT_ADDED_SINGLE))
  }
}
