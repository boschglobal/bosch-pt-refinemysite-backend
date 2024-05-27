/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCrParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitCsmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitFmParticipant
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
@DisplayName("State must be cleaned up on project deleted event")
@SmartSiteSpringBootTest
class CleanupStateFromProjectDeletedEventTest : BaseNotificationTest() {

  @Test
  fun `including notifications of all participants`() {
    eventStreamGenerator
        .submitProject()
      .submitProjectCraftG2()
        .submitCsmParticipant()
        .submitCrParticipant()
        .submitFmParticipant()
        .submitTaskAsFm()
        .submitTopicG2(auditUserReference = FM_USER)
        .submitMessage(auditUserReference = CSM_USER)

    // Check that notification where created for the csm
    assertThat(repositories.notificationRepository.findAll(getIdentifier(CSM_USER), 50)).isNotEmpty

    // Check that notification where created for the cr
    assertThat(repositories.notificationRepository.findAll(getIdentifier(CR_USER), 50)).isNotEmpty

    // Check that notification where created for the fm
    assertThat(repositories.notificationRepository.findAll(getIdentifier(FM_USER), 50)).isNotEmpty

    // Delete the project
    eventStreamGenerator.submitProject(eventType = ProjectEventEnumAvro.DELETED)

    // Check that no notifications exist anymore for the csm
    assertThat(repositories.notificationRepository.findAll(getIdentifier(CSM_USER), 50)).isEmpty()

    // Check that no notifications exist anymore for the cr
    assertThat(repositories.notificationRepository.findAll(getIdentifier(CR_USER), 50)).isEmpty()

    // Check that no notifications exist anymore for the fm
    assertThat(repositories.notificationRepository.findAll(getIdentifier(FM_USER), 50)).isEmpty()
  }
}
