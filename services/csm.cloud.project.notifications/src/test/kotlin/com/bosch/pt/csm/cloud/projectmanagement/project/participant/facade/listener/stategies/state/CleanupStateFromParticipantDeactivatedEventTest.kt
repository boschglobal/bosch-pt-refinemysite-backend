/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.stategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCrParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitFmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [RestDocumentationExtension::class, SpringExtension::class])
@DisplayName("State must be cleaned up on participant deactivated event")
@SmartSiteSpringBootTest
class CleanupStateFromParticipantDeactivatedEventTest : BaseNotificationTest() {

  @Test
  fun `including notifications of affected users`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitCsmParticipant()
        .submitCrParticipant()
        .submitFmParticipant()
        .submitTaskAsFm()
        .submitTopicG2(auditUserReference = FM_USER)
        .submitMessage(auditUserReference = CSM_USER)

    // Check that one notification for the csm and one for the fm user was created
    val notifications = repositories.notificationRepository.findAll()
    assertThat(notifications).isNotEmpty

    // Check that the participant of the cr is still active
    assertThat(getParticipant(CR_PARTICIPANT, PROJECT).active).isTrue

    // Check that the participant of the fm is inactive
    assertThat(getParticipant(FM_PARTICIPANT, PROJECT).active).isTrue

    // Get number of notifications for CR
    val numberOfNotificationsForCr =
        repositories.notificationRepository.findAll(getIdentifier(CR_USER), 50).size

    // Deactivate the fm participant
    eventStreamGenerator.submitParticipantG3(
        asReference = FM_PARTICIPANT, eventType = DEACTIVATED, auditUserReference = FM_USER)

    // Check that the notification of the cr still exist
    assertThat(repositories.notificationRepository.findAll(getIdentifier(CR_USER), 50))
        .hasSize(numberOfNotificationsForCr)

    // Check that the notifications of the fm were deleted
    assertThat(repositories.notificationRepository.findAll(getIdentifier(FM_USER), 50)).isEmpty()

    // Check that the participant of the cr is still active
    assertThat(getParticipant(CR_PARTICIPANT, PROJECT).active).isTrue

    // Check that the participant of the fm is inactive
    assertThat(getParticipant(FM_PARTICIPANT, PROJECT).active).isFalse
  }

  private fun getParticipant(participantReference: String, projectReference: String) =
      repositories.participantRepository.findOneByIdentifierAndProjectIdentifier(
          getIdentifier(participantReference), getIdentifier(projectReference))
}
