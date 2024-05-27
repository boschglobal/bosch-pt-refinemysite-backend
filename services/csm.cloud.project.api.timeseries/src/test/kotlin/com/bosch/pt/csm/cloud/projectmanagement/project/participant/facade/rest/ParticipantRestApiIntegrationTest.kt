/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.asCompanyId
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.asParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asRole
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.extension.asStatus
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class ParticipantRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: ParticipantAggregateG3Avro

  lateinit var aggregateV1: ParticipantAggregateG3Avro

  lateinit var aggregateOtherParticipant: ParticipantAggregateG3Avro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    submitEvents()
  }

  @Test
  fun `query participant`() {

    // Execute query
    val participantList = query(false)

    // Validate payload
    assertThat(participantList.participants).hasSize(3)

    val participantV0Id = aggregateV0.getIdentifier().asParticipantId()
    val participantV0 = participantList.participants.first { it.id == participantV0Id }
    assertThat(participantV0.version).isEqualTo(aggregateV0.getVersion())
    assertThat(participantV0.company)
        .isEqualTo(aggregateV0.company.identifier.toUUID().asCompanyId())
    assertThat(participantV0.user).isEqualTo(aggregateV0.user.identifier.toUUID().asUserId())
    assertThat(participantV0.role).isEqualTo(aggregateV0.role.asRole().key)
    assertThat(participantV0.status).isEqualTo(aggregateV0.status.asStatus().key)
    assertThat(participantV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())

    val participantV1 = participantList.participants.filter { it.id == participantV0Id }[1]
    assertThat(participantV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(participantV1.company)
        .isEqualTo(aggregateV1.company.identifier.toUUID().asCompanyId())
    assertThat(participantV1.user).isEqualTo(aggregateV1.user.identifier.toUUID().asUserId())
    assertThat(participantV1.role).isEqualTo(aggregateV1.role.asRole().key)
    assertThat(participantV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(participantV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())

    val otherParticipantId = aggregateOtherParticipant.getIdentifier().asParticipantId()
    val otherParticipant = participantList.participants.first { it.id == otherParticipantId }

    assertThat(otherParticipant.version).isEqualTo(aggregateOtherParticipant.getVersion())
    assertThat(otherParticipant.company)
        .isEqualTo(aggregateOtherParticipant.company.identifier.toUUID().asCompanyId())
    assertThat(otherParticipant.user)
        .isEqualTo(aggregateOtherParticipant.user.identifier.toUUID().asUserId())
    assertThat(otherParticipant.role).isEqualTo(aggregateOtherParticipant.role.asRole().key)
    assertThat(otherParticipant.status).isEqualTo(aggregateOtherParticipant.status.asStatus().key)
    assertThat(otherParticipant.eventTimestamp)
        .isEqualTo(aggregateOtherParticipant.eventTimestamp())
  }

  @Test
  fun `query participant latest only`() {

    // Execute query
    val participantList = query(true)

    // Validate payload
    assertThat(participantList.participants).hasSize(1)
    val participantV1 = participantList.participants.first()

    assertThat(participantV1.id).isEqualTo(aggregateV1.getIdentifier().asParticipantId())
    assertThat(participantV1.version).isEqualTo(aggregateV1.getVersion())
    assertThat(participantV1.company)
        .isEqualTo(aggregateV1.company.identifier.toUUID().asCompanyId())
    assertThat(participantV1.user).isEqualTo(aggregateV1.user.identifier.toUUID().asUserId())
    assertThat(participantV1.role).isEqualTo(aggregateV1.role.asRole().key)
    assertThat(participantV1.status).isEqualTo(aggregateV1.status.asStatus().key)
    assertThat(participantV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject()

    aggregateOtherParticipant =
        eventStreamGenerator
            .submitUser("other-user")
            .submitParticipantG3("other-participant") { it.status = INACTIVE }
            .get("other-participant")!!

    aggregateV0 = eventStreamGenerator.submitCsmParticipant().get("csm-participant")!!
    aggregateV1 =
        eventStreamGenerator
            .submitParticipantG3("csm-participant", eventType = ParticipantEventEnumAvro.UPDATED) {
              it.role = ParticipantRoleEnumAvro.CR
            }
            .get("csm-participant")!!
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/participants"),
          latestOnly,
          ParticipantListResource::class.java)
}
