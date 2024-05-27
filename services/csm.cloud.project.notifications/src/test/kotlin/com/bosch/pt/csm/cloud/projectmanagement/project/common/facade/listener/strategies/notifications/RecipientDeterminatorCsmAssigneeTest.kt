/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class RecipientDeterminatorCsmAssigneeTest : BaseNotificationTest() {

  @Test
  fun `Notifications are generated for the case of CSM assignee, to one FM, one CR and no other CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitCsmParticipant()
        .submitCrParticipant()
        .submitFmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCsm()

    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(0)
  }

  @Test
  fun `Notifications are generated for the case of CSM assignee, to one FM, one CR and other CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCsm()

    val notificationRecipients = setOf(fmUser.identifier, crUser.identifier, otherCsmUser.identifier)

    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(1)
    assertThat(
            repositories
                .notificationRepository
                .findAll()
                .filter { n ->
                  notificationRecipients.contains(n.notificationIdentifier.recipientIdentifier)
                }
                .count())
        .isEqualTo(1)
  }
}
