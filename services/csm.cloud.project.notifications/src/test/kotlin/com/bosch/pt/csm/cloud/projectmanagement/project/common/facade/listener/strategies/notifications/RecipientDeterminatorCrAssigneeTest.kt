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
class RecipientDeterminatorCrAssigneeTest : BaseNotificationTest() {

  @Test
  fun `Notifications are generated for the case of CR assignee, to one FM, no other CR and one CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCr()

    val notificationRecipients = setOf(csmUser.identifier, fmUser.identifier)
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

  @Test
  fun `Notifications are generated for the case of CR assignee, to one FM, other CR and one CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
        .submitOtherCrParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCr()

    val notificationRecipients = setOf(otherCrUser.identifier, csmUser.identifier, fmUser.identifier)
    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(2)
    assertThat(
            repositories
                .notificationRepository
                .findAll()
                .filter { n ->
                  notificationRecipients.contains(n.notificationIdentifier.recipientIdentifier)
                }
                .count())
        .isEqualTo(2)
  }

  @Test
  fun `Notifications are generated for the case of CR assignee, to one FM, no other CR and multiple CSMs`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCr()

    val notificationRecipients = setOf(csmUser.identifier, otherCsmUser.identifier, fmUser.identifier)
    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(2)
    assertThat(
            repositories
                .notificationRepository
                .findAll()
                .filter { n ->
                  notificationRecipients.contains(n.notificationIdentifier.recipientIdentifier)
                }
                .count())
        .isEqualTo(2)
  }

  @Test
  fun `Notifications are generated for the case of CR assignee, to one FM, other CR and multiple CSMs`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitOtherCrParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsCr()

    val notificationRecipients =
        setOf(otherCrUser.identifier, csmUser.identifier, otherCsmUser.identifier, fmUser.identifier)

    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(3)
    assertThat(
            repositories
                .notificationRepository
                .findAll()
                .filter { n ->
                  notificationRecipients.contains(n.notificationIdentifier.recipientIdentifier)
                }
                .count())
        .isEqualTo(3)
  }
}
