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
class RecipientDeterminatorFmAssigneeTest : BaseNotificationTest() {

  @Test
  fun `Notifications are generated for the case of FM assignee, to zero CRs and one CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    val notificationRecipients = setOf(csmUser.identifier)

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
  fun `Notifications are generated for the case of FM assignee, to one CR and one CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    val notificationRecipients = setOf(crUser.identifier, csmUser.identifier)

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
  fun `Notifications are generated for the case of FM assignee, to multiple CRs and one CSM`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitOtherCrParticipant()
        .submitCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    val notificationRecipients =
        setOf(crUser.identifier, otherCrUser.identifier, csmUser.identifier)

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

  @Test
  fun `Notifications are generated for the case of FM assignee, to zero CR and multiple CSMs`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    val notificationRecipients = setOf(csmUser.identifier, otherCsmUser.identifier)

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
  fun `Notifications are generated for the case of FM assignee, to one CR and multiple CSMs`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    var notificationRecipients =
        setOf(crUser.identifier, csmUser.identifier, otherCsmUser.identifier)

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

  @Test
  fun `Notifications are generated for the case of FM assignee, to multiple CRs and multiple CSMs`() {
    eventStreamGenerator
        .submitProject()
        .submitProjectCraftG2()
        .submitFmParticipant()
        .submitCrParticipant()
        .submitOtherCrParticipant()
        .submitCsmParticipant()
        .submitOtherCsmParticipant()
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAsFm()

    val notificationRecipients =
        setOf(
            crUser.identifier, otherCrUser.identifier, csmUser.identifier, otherCsmUser.identifier)

    assertThat(repositories.notificationRepository.findAll().count()).isEqualTo(4)
    assertThat(
            repositories
                .notificationRepository
                .findAll()
                .filter { n ->
                  notificationRecipients.contains(n.notificationIdentifier.recipientIdentifier)
                }
                .count())
        .isEqualTo(4)
  }
}
