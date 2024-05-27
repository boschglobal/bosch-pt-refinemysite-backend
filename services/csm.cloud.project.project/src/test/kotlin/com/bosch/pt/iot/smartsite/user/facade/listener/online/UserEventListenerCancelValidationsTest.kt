/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * This class tests the removal of an invited participant that is still be in validation. When a
 * user is deleted participants that are in status "validation" for this user are physically deleted
 * since they cannot be referenced anywhere.
 */
@EnableAllKafkaListeners
open class UserEventListenerCancelValidationsTest : AbstractIntegrationTestV2() {

  private lateinit var participantProject1: Participant
  private lateinit var participantProject2: Participant

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .plainProjectWithCsm()
        .submitUser("mustermann")
        .submitParticipantG3(asReference = "project1-fm") {
          it.role = FM
          it.status = VALIDATION
          it.company = null
          it.user = getByReference("mustermann")
        }
        .submitInvitation(asReference = "project1-fm-invitation") {
          it.participantIdentifier = getIdentifier("project1-fm").toString()
          it.projectIdentifier = getIdentifier("project").toString()
          it.email = get<UserAggregateAvro>("mustermann")!!.getEmail()
        }
        .submitProject(asReference = "project2")
        .submitParticipantG3(asReference = "project2-csm") {
          it.role = CSM
          it.status = ACTIVE
          it.user = getByReference("csm-user")
        }
        .submitParticipantG3(asReference = "project2-fm") {
          it.role = FM
          it.status = VALIDATION
          it.company = null
          it.user = getByReference("mustermann")
        }
        .submitInvitation(asReference = "project2-fm-invitation") {
          it.participantIdentifier = getIdentifier("project2-fm").toString()
          it.projectIdentifier = getIdentifier("project2").toString()
          it.email = get<UserAggregateAvro>("mustermann")!!.getEmail()
        }

    setAuthentication(getIdentifier("csm-user"))
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()

    participantProject1 = repositories.findParticipant(getIdentifier("project1-fm"))!!
    participantProject2 = repositories.findParticipant(getIdentifier("project2-fm"))!!
    useOnlineListener()
  }

  @Test
  fun `validate participant in validation is deleted when user tombstone message is received`() {
    eventStreamGenerator.submitUserTombstones(reference = "mustermann")

    repositories.participantRepository.findOneByIdentifier(participantProject1.identifier).also {
      assertThat(it).isNull()
    }

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, CANCELLED, 2)
        .also {
          validateParticipantDeleted(it[0].getAggregate(), participantProject1)
          validateParticipantDeleted(it[1].getAggregate(), participantProject2)
        }
  }

  private fun validateParticipantDeleted(
      aggregate: ParticipantAggregateG3Avro,
      participant: Participant
  ) {
    val expectedIdentifier =
        AggregateIdentifierAvro.newBuilder()
            .setIdentifier(participant.identifier.toString())
            .setType(PARTICIPANT.value)
            .setVersion(participant.version + 1)
            .build()
    assertThat(aggregate.getAggregateIdentifier()).isEqualByComparingTo(expectedIdentifier)
    assertThat(aggregate.getStatus()).isEqualTo(VALIDATION)
  }
}
