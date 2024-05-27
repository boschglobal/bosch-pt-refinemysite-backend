/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsCr
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TopicCreatedNotificationStrategyG2Test : BaseNotificationStrategyTest() {

  @Test
  fun `Notifications are generated without description if they do not contain text`() {
    eventStreamGenerator.submitTaskAsCr()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTopicG2(auditUserReference = CR_USER)
    repositories.notificationRepository.findAll().also {
      assertThat(it).hasSize(1)

      checkNotificationForTopicCreatedEventG2(
          notification = it.first(),
          requestUser = csmUser,
          actorUser = crUserAggregate,
          actorParticipant = crParticipantAggregate)
    }
  }

  @Test
  fun `Notifications are not generated for deactivated participants`() {
    eventStreamGenerator
        .submitTaskAsCr()
        .submitParticipantG3(
            eventType = ParticipantEventEnumAvro.DEACTIVATED, asReference = CR_PARTICIPANT)
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTopicG2()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `Notifications are not generated for tasks in status draft`() {
    eventStreamGenerator.submitTask(auditUserReference = CR_USER) {
      it.assignee = getByReference(CR_PARTICIPANT)
      it.status = TaskStatusEnumAvro.DRAFT
    }

    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTopicG2()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `event Listener is idempotent and does not create notification duplicates`() {
    eventStreamGenerator.submitTaskAsCr()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.repeat { eventStreamGenerator.submitTopicG2() }
    assertThat(repositories.notificationRepository.findAll()).hasSize(1)
  }
}
