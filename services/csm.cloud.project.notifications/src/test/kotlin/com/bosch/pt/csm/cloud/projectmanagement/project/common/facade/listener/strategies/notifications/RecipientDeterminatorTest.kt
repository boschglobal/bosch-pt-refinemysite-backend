/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class RecipientDeterminatorTest : BaseNotificationStrategyTest() {

  @Test
  fun `Notifications are not generated for deactivated participants`() {
    eventStreamGenerator
        .submitTaskAsCr()
        .submitTaskSchedule(auditUserReference = CR_USER)
        .submitParticipantG3(
            asReference = CR_PARTICIPANT, eventType = ParticipantEventEnumAvro.DEACTIVATED)
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitDayCardG2()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `Notifications are not generated for tasks in status draft`() {
    eventStreamGenerator.submitTask {
      it.assignee = getByReference(CR_PARTICIPANT)
      it.status = TaskStatusEnumAvro.DRAFT
    }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitDayCardG2().submitTaskSchedule()
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }
}
