/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The email attribute that is currently maintained on the [Participant] is copied either from the
 * invitation or from the user (once an invitation has been accepted). It is used for sorting the
 * participant list by email address. The [Participant] class is therefore also used as read-view at
 * the moment. Restoring the [Participant] from the event stream can result in different scenarios
 * since user, invitation and participant events are distributed over three kafka topics. This test
 * covers these scenarios.
 */
class RestoreEmailOnParticipantSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val userIdentifier = randomUUID()
  private val participantIdentifier = randomUUID()
  private val email = "participant@example.com"

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
  }

  @Test
  fun `scenario Invitation-Participant-User-Participant`() {
    eventStreamGenerator
        .submitInvitationForUser()
        .submitParticipantInvited()
        .submitUserThatAcceptedInvitation()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario User-Participant-Invitation-Participant`() {
    eventStreamGenerator
        .submitUserThatAcceptedInvitation()
        .submitParticipantInvited()
        .submitInvitationForUser()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-Invitation-Participant-User`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitInvitationForUser()
        .submitParticipantInValidation()
        .submitUserThatAcceptedInvitation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-User-Participant-Invitation`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitUserThatAcceptedInvitation()
        .submitParticipantInValidation()
        .submitInvitationForUser()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario User-Invitation-Participant-Participant`() {
    eventStreamGenerator
        .submitUserThatAcceptedInvitation()
        .submitInvitationForUser()
        .submitParticipantInvited()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Invitation-User-Participant-Participant`() {
    eventStreamGenerator
        .submitUserThatAcceptedInvitation()
        .submitInvitationForUser()
        .submitParticipantInvited()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-Invitation-User-Participant`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitInvitationForUser()
        .submitUserThatAcceptedInvitation()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-User-Invitation-Participant`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitUserThatAcceptedInvitation()
        .submitInvitationForUser()
        .submitParticipantInValidation()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-Participant-User-Invitation`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitParticipantInValidation()
        .submitUserThatAcceptedInvitation()
        .submitInvitationForUser()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario Participant-Participant-Invitation-User`() {
    eventStreamGenerator
        .submitParticipantInvited()
        .submitParticipantInValidation()
        .submitUserThatAcceptedInvitation()
        .submitInvitationForUser()

    repositories.participantRepository
        .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
          assertThat(it.email).isEqualTo(email)
        }
  }

  @Test
  fun `scenario User-Participant-Participant-Invitation`() {
    eventStreamGenerator
      .submitUserThatAcceptedInvitation()
      .submitParticipantInvited()
      .submitParticipantInValidation()
      .submitInvitationForUser()

    repositories.participantRepository
      .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
        assertThat(it.email).isEqualTo(email)
      }
  }

  @Test
  fun `scenario Invitation-Participant-Participant-User`() {
    eventStreamGenerator
      .submitInvitationForUser()
      .submitParticipantInvited()
      .submitParticipantInValidation()
      .submitUserThatAcceptedInvitation()

    repositories.participantRepository
      .findOneByIdentifier(participantIdentifier.asParticipantId())!!.also {
        assertThat(it.email).isEqualTo(email)
      }
  }

  private fun EventStreamGenerator.submitInvitationForUser() = submitInvitation {
    it.email = email
    it.participantIdentifier = participantIdentifier.toString()
  }

  private fun EventStreamGenerator.submitParticipantInvited() =
      submitParticipantG3("invited-participant") {
        it.status = INVITED
        it.aggregateIdentifier =
            AggregateIdentifierAvro.newBuilder()
                .setType(ProjectmanagementAggregateTypeEnum.PARTICIPANT.name)
                .setIdentifier(participantIdentifier.toString())
                .setVersion(0)
                .build()
      }

  private fun EventStreamGenerator.submitParticipantInValidation() =
      submitParticipantG3("invited-participant") {
        it.status = VALIDATION
        it.user =
            AggregateIdentifierAvro.newBuilder()
                .setType(USER.name)
                .setIdentifier(userIdentifier.toString())
                .setVersion(0)
                .build()
        it.company = null
      }

  private fun EventStreamGenerator.submitUserThatAcceptedInvitation() =
      submitUser("invited-user") {
        it.aggregateIdentifier =
            AggregateIdentifierAvro.newBuilder()
                .setType(USER.name)
                .setIdentifier(userIdentifier.toString())
                .setVersion(0)
                .build()
        it.email = email
      }
}
