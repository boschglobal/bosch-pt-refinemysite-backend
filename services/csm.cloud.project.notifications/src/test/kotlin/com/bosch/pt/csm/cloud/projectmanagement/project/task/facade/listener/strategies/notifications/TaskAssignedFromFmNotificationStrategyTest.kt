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
@DisplayName("When a task, assigned to a participant being an FM,")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAssignedFromFmNotificationStrategyTest : BaseNotificationStrategyTest() {

  @BeforeEach
  fun createAndSentTaskAssignedToFm() {
    eventStreamGenerator
        .submitTask(auditUserReference = FM_USER) {
          it.status = TaskStatusEnumAvro.DRAFT
          it.assignee = getByReference(FM_PARTICIPANT)
        }
        .submitTask(auditUserReference = FM_USER, eventType = TaskEventEnumAvro.SENT) {
          it.status = TaskStatusEnumAvro.OPEN
        }
    repositories.notificationRepository.deleteAll()
  }

  @Test
  fun `is reassigned by the CSM to himself, no notifications are generated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ASSIGNED) {
          it.assignee = getByReference(CSM_PARTICIPANT)
        }
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  @Test
  fun `is reassigned by the CSM to the CR, the CR is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ASSIGNED) {
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
  fun `is reassigned by the CR to himself, the CSM is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CR_USER, eventType = TaskEventEnumAvro.ASSIGNED) {
          it.assignee = getByReference(CR_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = csmUser,
          actorUser = crUserAggregate,
          actorParticipant = crParticipantAggregate,
          assigneeUser = crUserAggregate,
          assigneeParticipant = crParticipantAggregate)
    }
  }

  @Test
  fun `is reassigned by the CSM to a CR of another company, the new CR is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ASSIGNED) {
          it.assignee = getByReference(OTHER_COMPANY_CR_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = otherCompanyCrUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `is reassigned by the CSM to an FM of another company, the new CR and the FM are notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.ASSIGNED) {
          it.assignee = getByReference(OTHER_COMPANY_FM_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(otherCompanyCrUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = otherCompanyCrUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            assigneeUser = otherCompanyFmUserAggregate,
            assigneeParticipant = otherCompanyFmParticipantAggregate)
      }

      notifications.selectFirstFor(otherCompanyFmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = otherCompanyFmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }

  @Test
  fun `is updated by the CSM and reassigned to himself, no notifications are generated`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.UPDATED) {
          it.assignee = getByReference(CSM_PARTICIPANT)
        }

    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).isEmpty()
  }

  @Test
  fun `is updated by the CSM and reassigned to the CR, the CR is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.UPDATED) {
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
  fun `is updated by the CR and reassigned to himself, the CSM is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CR_USER, eventType = TaskEventEnumAvro.UPDATED) {
          it.assignee = getByReference(CR_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = csmUser,
          actorUser = crUserAggregate,
          actorParticipant = crParticipantAggregate,
          assigneeUser = crUserAggregate,
          assigneeParticipant = crParticipantAggregate)
    }
  }

  @Test
  fun `is updated by the CSM and reassigned to CR of another company, the new CR is notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.UPDATED) {
          it.assignee = getByReference(OTHER_COMPANY_CR_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().single().also {
      checkNotificationForTaskAssignedEvent(
          notification = it,
          requestUser = otherCompanyCrUser,
          actorUser = csmUserAggregate,
          actorParticipant = csmParticipantAggregate)
    }
  }

  @Test
  fun `is updated by the CSM and reassigned to FM of another company, the new CR and the FM are notified`() {
    eventStreamGenerator.submitTask(
        auditUserReference = CSM_USER, eventType = TaskEventEnumAvro.UPDATED) {
          it.assignee = getByReference(OTHER_COMPANY_FM_PARTICIPANT)
        }

    repositories.notificationRepository.findAll().also { notifications ->
      assertThat(notifications).hasSize(2)

      notifications.selectFirstFor(otherCompanyCrUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = otherCompanyCrUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate,
            assigneeUser = otherCompanyFmUserAggregate,
            assigneeParticipant = otherCompanyFmParticipantAggregate)
      }

      notifications.selectFirstFor(otherCompanyFmUser).also {
        checkNotificationForTaskAssignedEvent(
            notification = it,
            requestUser = otherCompanyFmUser,
            actorUser = csmUserAggregate,
            actorParticipant = csmParticipantAggregate)
      }
    }
  }
}