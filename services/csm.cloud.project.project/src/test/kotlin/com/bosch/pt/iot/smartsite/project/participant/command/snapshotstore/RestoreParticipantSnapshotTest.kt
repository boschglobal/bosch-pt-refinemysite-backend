/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.REACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreParticipantSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val user by lazy { repositories.findUser(getIdentifier("user"))!! }
  private val participantAggregate by lazy { get<ParticipantAggregateG3Avro>(PARTICIPANT_REF)!! }
  private val participant by lazy { repositories.findParticipant(getIdentifier(PARTICIPANT_REF))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    projectEventStoreUtils.reset()
  }

  @Test
  open fun `validate that participant created event was processed successfully`() {
    validateBasicAttributes(participant, participantAggregate, user.email!!)
    validateAuditingInformationAndIdentifierAndVersion(participant, participantAggregate)
  }

  @Test
  open fun `validate that participant updated event was processed successfully`() {
    eventStreamGenerator.submitParticipantG3(PARTICIPANT_REF, eventType = UPDATED) { it.role = CSM }

    validateBasicAttributes(participant, participantAggregate, user.email!!)
    validateAuditingInformationAndIdentifierAndVersion(participant, participantAggregate)
  }

  @Test
  open fun `validate participant deactivated event deactivates a participant`() {
    eventStreamGenerator.submitParticipantG3(PARTICIPANT_REF, eventType = DEACTIVATED) {
      it.status = INACTIVE
    }

    assertThat(participant.isActive()).isFalse
    validateBasicAttributes(participant, participantAggregate, user.email!!)
    validateAuditingInformationAndIdentifierAndVersion(participant, participantAggregate)
  }

  @Test
  open fun `validate participant reactivated event activates a participant`() {
    eventStreamGenerator
        .submitParticipantG3(PARTICIPANT_REF, eventType = DEACTIVATED) { it.status = INACTIVE }
        .submitParticipantG3(PARTICIPANT_REF, eventType = REACTIVATED) { it.status = ACTIVE }

    assertThat(participant.isActive()).isTrue
    validateBasicAttributes(participant, participantAggregate, user.email!!)
    validateAuditingInformationAndIdentifierAndVersion(participant, participantAggregate)
  }

  @Test
  open fun `validate participant cancelled event deletes invited participant`() {
    val participantRef = "validation"

    eventStreamGenerator.submitParticipantG3(participantRef) { it.status = INVITED }

    assertThat(repositories.findParticipant(getIdentifier(participantRef))).isNotNull

    eventStreamGenerator.submitParticipantG3(participantRef, eventType = CANCELLED)

    assertThat(repositories.findParticipant(getIdentifier(participantRef))).isNull()
  }

  @Test
  open fun `validate participant cancelled event deletes participant in validation`() {
    val participantRef = "validation"

    eventStreamGenerator.submitParticipantG3(participantRef) { it.status = VALIDATION }

    assertThat(repositories.findParticipant(getIdentifier(participantRef))).isNotNull

    eventStreamGenerator.submitParticipantG3(participantRef, eventType = CANCELLED)

    assertThat(repositories.findParticipant(getIdentifier(participantRef))).isNull()
  }

  private fun validateBasicAttributes(
      participant: Participant,
      participantAggregate: ParticipantAggregateG3Avro,
      email: String
  ) {
    assertThat(participant.company!!.identifier)
        .isEqualTo(participantAggregate.getCompanyIdentifier())
    assertThat(participant.user!!.identifier).isEqualTo(participantAggregate.getUserIdentifier())
    assertThat(participant.project!!.identifier)
        .isEqualTo(participantAggregate.getProjectIdentifier().asProjectId())
    assertThat(participant.role!!.name).isEqualTo(participantAggregate.role.name)
    assertThat(participant.email).isEqualTo(email)
  }

  companion object {
    private const val PARTICIPANT_REF = "participant"
  }
}
