/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.stategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitFmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [RestDocumentationExtension::class, SpringExtension::class])
@DisplayName("State must be updated on participant")
@SmartSiteSpringBootTest
class ParticipantReassignedTest : BaseNotificationTest() {

  @Autowired private lateinit var notificationController: NotificationController

  @BeforeEach
  override fun setup() {
    super.setup()
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitCsmParticipant()
        .submitFmParticipant()
        .submitTaskAsFm()
        .submitTopicG2(auditUserReference = FM_USER)
        .submitMessage(auditUserReference = CSM_USER)
    setFakeUrlWithApiVersion()

    // Check that one notification for the csm and one for the fm user was created
    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).isNotEmpty

    // Check that the participant of the fm is active
    assertThat(getParticipant(FM_PARTICIPANT, PROJECT).active).isTrue

    // Get number of notifications for fm
    val userIdentifier = getIdentifier(FM_USER)
    val numberOfNotificationsForFm =
        repositories.notificationRepository.findAll(userIdentifier, 50).size

    assertThat(numberOfNotificationsForFm).isEqualTo(1)

    // Deactivate the fm participant
    eventStreamGenerator.submitParticipantG3(
        asReference = FM_PARTICIPANT, eventType = ParticipantEventEnumAvro.DEACTIVATED)

    // Check that the notifications of the fm were deleted
    assertThat(repositories.notificationRepository.findAll(userIdentifier, 50)).isEmpty()

    // Check that the participant of the fm is inactive
    assertThat(getParticipant(FM_PARTICIPANT, PROJECT).active).isFalse

    // Check that CSM can still retrieve notifications where the actor is the inactive FM
    val csmUser = repositories.userRepository.findOneCachedByIdentifier(getIdentifier(CSM_USER))
    val notificationsForCsm =
        notificationController.findAllNotificationsForUser(csmUser!!, 10).body!!
    assertThat(notificationsForCsm.items.size).isEqualTo(2)
    assertThat(notificationsForCsm.items.last().actor.identifier)
        .isEqualTo(getIdentifier(FM_PARTICIPANT))
  }

  @Test
  fun `re-assignment to the same company including notifications`() {
    // Re-assign the user to the same company
    eventStreamGenerator
        .submitEmployee(asReference = FM_EMPLOYEE, eventType = EmployeeEventEnumAvro.DELETED)
        // use a different "asReference" here because re-assignment will produce a new employee with
        // a different employee identifier
        .submitEmployee(asReference = "reassigned-employee")

    // Reactivate participant for the new employee and generate notifications. Because the user was
    // re-assigned to the same company, the user's participant can be REACTIVATED, thereby keeping
    // the participant identifier.
    eventStreamGenerator
        .submitParticipantG3(
            asReference = FM_PARTICIPANT, eventType = ParticipantEventEnumAvro.REACTIVATED)
        .submitTask(asReference = "task-new", auditUserReference = FM_USER) {
          it.assignee = getByReference(FM_PARTICIPANT)
        }
        .submitTopicG2(asReference = "topic-new", auditUserReference = FM_USER)
        .submitMessage(asReference = "message-new", auditUserReference = CSM_USER)

    // Check that notifications are created for the new participant of the fm-user correctly
    repositories.notificationRepository.findAll(getIdentifier(FM_USER), 50).size.apply {
      assertThat(this).isEqualTo(1)
    }
  }

  @Test
  fun `re-assignment to another company including notifications`() {
    // Re-assign the user to another company
    eventStreamGenerator
        .submitEmployee(asReference = FM_EMPLOYEE, eventType = EmployeeEventEnumAvro.DELETED)
        // use a different "asReference" here because re-assignment will produce a new employee with
        // a different employee identifier
        .submitEmployee(asReference = "reassigned-employee") {
          it.company = getByReference(COMPANY_2)
        }

    // Create new participant for the new employee and generate notifications. Because the user was
    // re-assigned to a different company, it is not possible to REACTIVATE the previous
    // participant.
    eventStreamGenerator
        .submitParticipantG3(
            asReference = "fm-participant-new", eventType = ParticipantEventEnumAvro.CREATED) {
          it.user = getByReference(FM_USER)
        }
        .submitTask(asReference = "task-new", auditUserReference = FM_USER) {
          it.assignee = getByReference("fm-participant-new")
        }
        .submitTopicG2(asReference = "topic-new", auditUserReference = FM_USER)
        .submitMessage(asReference = "message-new", auditUserReference = CSM_USER)

    // Check that notifications are created for the new participant of the fm-user correctly
    repositories.notificationRepository.findAll(getIdentifier(FM_USER), 50).size.apply {
      assertThat(this).isEqualTo(1)
    }
  }

  private fun getParticipant(participantReference: String, projectReference: String) =
      repositories.participantRepository.findOneByIdentifierAndProjectIdentifier(
          getIdentifier(participantReference), getIdentifier(projectReference))
}
