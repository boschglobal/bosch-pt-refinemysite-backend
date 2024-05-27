/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.api.toAggregateReference
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.RESENT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitationTombstones
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asInvitationId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Invitation
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import java.time.Instant.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreInvitationSnapshotTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun setupBaseData() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
  }

  @Test
  fun `validate invitation created event`() {
    eventStreamGenerator.submitInvitation()

    val invitation = get<InvitationAggregateAvro>("invitation")!!

    repositories.invitationRepository
        .findOneByIdentifier(getIdentifier("invitation").asInvitationId())!!
        .apply {
          validateAuditingInformationAndIdentifierAndVersion(this, invitation)
          validateAttributes(this, invitation)
        }

    repositories.participantRepository
        .findOneByIdentifier(invitation.getParticipantIdentifier().asParticipantId())!!
        .apply { assertThat(email).isEqualTo(invitation.getEmail()) }
  }

  @Test
  fun `validate invitation resent event`() {
    eventStreamGenerator
        .submitInvitation(time = now().minusSeconds(100))
        .submitInvitation(time = now(), eventType = RESENT)

    val invitation = get<InvitationAggregateAvro>("invitation")!!

    repositories.invitationRepository
        .findOneByIdentifier(invitation.getIdentifier().asInvitationId())!!
        .apply {
          validateAuditingInformationAndIdentifierAndVersion(this, invitation)
          validateAttributes(this, invitation)
        }
  }

  @Test
  fun `validate invitation tombstone event deletes invitation`() {
    eventStreamGenerator
        .submitInvitation(time = now().minusSeconds(100))
        .submitInvitation(time = now(), eventType = RESENT)

    val invitation = get<InvitationAggregateAvro>("invitation")!!

    repositories.invitationRepository
        .findOneByIdentifier(invitation.getIdentifier().asInvitationId())
        .apply { assertThat(this).isNotNull }

    eventStreamGenerator.submitInvitationTombstones()

    repositories.invitationRepository
        .findOneByIdentifier(invitation.getIdentifier().asInvitationId())
        .apply { assertThat(this).isNull() }
  }

  @Test
  fun `validate invitation tombstone event deletes participant and invitation`() {
    eventStreamGenerator
        .submitParticipantG3 {
          it.status = ParticipantStatusEnumAvro.INVITED
          it.user = randomUUID().toString().asUserId().toAggregateReference()
        }
        .submitInvitation { it.participantIdentifier = getIdentifier("participant").toString() }

    val invitation = get<InvitationAggregateAvro>("invitation")!!
    val participant = get<ParticipantAggregateG3Avro>("participant")!!

    repositories.invitationRepository
        .findOneByIdentifier(invitation.getIdentifier().asInvitationId())
        .apply { assertThat(this).isNotNull }
    repositories.participantRepository
        .findOneByIdentifier(participant.getIdentifier().asParticipantId())
        .apply { assertThat(this).isNotNull }

    eventStreamGenerator.submitInvitationTombstones()

    repositories.invitationRepository
        .findOneByIdentifier(invitation.getIdentifier().asInvitationId())
        .apply { assertThat(this).isNull() }
    repositories.participantRepository
        .findOneWithDetailsByIdentifier(participant.getIdentifier().asParticipantId())
        .apply { assertThat(this).isNull() }
  }

  @Test
  fun `validate invitation deleted event is no longer supported`() {
    eventStreamGenerator
        .submitParticipantG3 {
          it.status = ParticipantStatusEnumAvro.INVITED
          it.user = randomUUID().toString().asUserId().toAggregateReference()
        }
        .submitInvitation { it.participantIdentifier = getIdentifier("participant").toString() }

    assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
      eventStreamGenerator.submitInvitation(eventType = DELETED) {
        it.participantIdentifier = getIdentifier("participant").toString()
      }
    }
  }

  private fun validateAttributes(entity: Invitation, aggregate: InvitationAggregateAvro) {
    with(entity) {
      assertThat(projectIdentifier).isEqualTo(aggregate.projectIdentifier.asProjectId())
      assertThat(participantIdentifier).isEqualTo(aggregate.participantIdentifier.asParticipantId())
      assertThat(email).isEqualTo(aggregate.email)
      assertThat(lastSent).isEqualTo(aggregate.lastSent.toLocalDateTimeByMillis())
    }
  }
}
