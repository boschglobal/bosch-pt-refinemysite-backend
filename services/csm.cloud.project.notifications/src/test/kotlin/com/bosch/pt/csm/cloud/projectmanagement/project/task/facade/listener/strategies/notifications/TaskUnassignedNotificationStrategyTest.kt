/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.google.common.collect.Sets.difference
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskUnassignedNotificationStrategyTest : BaseNotificationStrategyTest() {

  @Test
  fun `verify notifications are not generated for unassigned Event when task status is draft`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.DRAFT
    }
    val notificationsBeforeUnassignedEvent =
        repositories.notificationRepository.findAll().map { it.externalIdentifier }.toSet()

    unassignUserFromTask()

    assertThatNoNewNotificationsCreated(notificationsBeforeUnassignedEvent)
  }

  @Test
  fun `verify notifications are not generated for unassigned event when previous assignee is inactive`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = getByReference(FM_PARTICIPANT_INACTIVE)
      it.status = TaskStatusEnumAvro.OPEN
    }
    val notificationsBeforeUnassignedEvent =
        repositories.notificationRepository.findAll().map { it.externalIdentifier }.toSet()

    unassignUserFromTask()

    assertThatNoNewNotificationsCreated(notificationsBeforeUnassignedEvent)
  }

  @ParameterizedTest
  @EnumSource(TaskStatusEnumAvro::class, names = ["OPEN", "STARTED", "CLOSED"])
  fun `verify notifications are generated for unassigned event given task status`(
      status: TaskStatusEnumAvro
  ) {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = status
    }

    unassignUserFromTask()

    checkNotificationForTaskUnassignedEvent(
        notification = repositories.notificationRepository.findAll().selectFirstFor(fmUser),
        requestUser = fmUser,
        actorUser = csmUserAggregate,
        actorParticipant = csmParticipantAggregate)
  }

  private fun assertThatNoNewNotificationsCreated(previouslyExistingNotifications: Set<UUID?>) {
    val newNotifications =
        difference(
            repositories.notificationRepository.findAll().map { it.externalIdentifier }.toSet(),
            previouslyExistingNotifications)
    assertThat(newNotifications).isEmpty()
  }

  private fun unassignUserFromTask() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.UNASSIGNED) {
      it.assignee = null
    }
  }

  private fun checkNotificationForTaskUnassignedEvent(
      notification: Notification,
      requestUser: User,
      actorUser: UserAggregateAvro,
      actorParticipant: ParticipantAggregateG3Avro
  ) {
    val actorDisplayName = "${actorUser.getFirstName()} ${actorUser.getLastName()}"

    checkNotifications(
        notification = notification,
        requestUser = requestUser,
        actorUser = actorUser,
        actorParticipant = actorParticipant,
        project = projectAggregate,
        task = taskAggregate,
        objectReference = taskAggregate.buildObjectReference(),
        summary =
            NotificationSummaryDto(
                Key.NOTIFICATION_SUMMARY_UNASSIGNED_TASK_FROM_YOU,
                mapOf(
                    "originator" to
                        PlaceholderValueDto(
                            "PARTICIPANT", actorParticipant.getIdentifier(), actorDisplayName))),
        details = null)
  }
}
