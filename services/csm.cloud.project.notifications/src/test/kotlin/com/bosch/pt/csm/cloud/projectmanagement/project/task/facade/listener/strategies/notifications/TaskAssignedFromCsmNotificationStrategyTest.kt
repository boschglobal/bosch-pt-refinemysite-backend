/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.selectFirstFor
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

/*
 * The notifications for the task assigned can be generated by the following events: CREATE, SENT,
 * ASSIGNED and UPDATED. However, for testing purpose, the SENT and ASSIGNED can be considered the same.
 */
@DisplayName("When a task, assigned to a participant being a CSM,")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAssignedFromCsmNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createTaskAssignedToCsm() {
    eventStreamGenerator
        .submitTask(auditUserReference = CSM_USER) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.assignee = getByReference(CSM_PARTICIPANT)
        }
        .submitTask(auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
        }
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `is sent by the CSM themselves to the CR, the CR is notified`() {
    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.SENT) {
      it.assignee = getByReference(CR_PARTICIPANT)
    }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = crUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `is sent by the CSM themselves to the FM, the CR and the FM (being the assignee) are notified`() {
    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.SENT) {
      it.assignee = getByReference(FM_PARTICIPANT)
    }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            assigneeUser = fmUserAggregate,
            assigneeParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }

  @Test
  fun `is updated by the CSM themselves to the CR, the CR is notified`() {
    eventStreamGenerator.submitTask(eventType = TaskEventEnumAvro.UPDATED) {
      it.assignee = getByReference(CR_PARTICIPANT)
    }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = crUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `is updated by the CSM themselves to the FM, the CR and the FM (being the assignee) are notified`() {
    eventStreamGenerator
        .submitTask(eventType = TaskEventEnumAvro.SENT) {
          it.assignee = getByReference(CSM_PARTICIPANT)
        }
        .submitTask(eventType = TaskEventEnumAvro.SENT) {
          it.assignee = getByReference(FM_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(crUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = crUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            assigneeUser = fmUserAggregate,
            assigneeParticipant = fmParticipantAggregate)
      }

      notifications.selectFirstFor(fmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = fmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }
}
