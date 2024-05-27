/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.api.toAggregateReference
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * This class tests the acceptance of an invitation by receiving a CREATED or REGISTERED event for a
 * user that was invited to one or many projects. The participant status should change to "IN
 * VALIDATION".
 */
@EnableAllKafkaListeners
open class UserEventListenerAcceptInvitationsTest : AbstractIntegrationTestV2() {

  private val participant1Project1 by lazy {
    repositories.findParticipant(getIdentifier("participant1Project1"))!!
  }
  private val participant1Project2 by lazy {
    repositories.findParticipant(getIdentifier("participant1Project2"))!!
  }

  @BeforeEach
  fun init() {

    eventStreamGenerator
        .setupDatasetTestData()
        .submitParticipantG3(asReference = "participant1Project1") {
          it.role = FM
          it.status = INVITED
          it.company = null
          it.user = null
        }
        .submitParticipantG3(asReference = "participant2Project1") {
          it.role = CR
          it.status = INVITED
          it.company = null
          it.user = null
        }
        .submitInvitation(asReference = "invitation1Project1") {
          it.participantIdentifier = getIdentifier("participant1Project1").toString()
          it.projectIdentifier = getIdentifier("project").toString()
          it.email = EMAIL_1
        }
        .submitInvitation(asReference = "invitation2Project1") {
          it.participantIdentifier = getIdentifier("participant2Project1").toString()
          it.projectIdentifier = getIdentifier("project").toString()
          it.email = EMAIL_2
        }
        .submitProject(asReference = "project2")
        .submitParticipantG3("participantCsm1Project2") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(asReference = "participant1Project2") {
          it.role = FM
          it.status = INVITED
          it.company = null
          it.user = null
        }
        .submitParticipantG3(asReference = "participant2Project2") {
          it.role = CR
          it.status = INVITED
          it.company = null
          it.user = null
        }
        .submitInvitation(asReference = "invitation1Project2") {
          it.participantIdentifier = getIdentifier("participant1Project2").toString()
          it.projectIdentifier = getIdentifier("project2").toString()
          it.email = EMAIL_1
        }
        .submitInvitation(asReference = "invitation2Project2") {
          it.participantIdentifier = getIdentifier("participant2Project2").toString()
          it.projectIdentifier = getIdentifier("project2").toString()
          it.email = EMAIL_2
        }

    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
    useOnlineListener()
  }

  @Test
  open fun `validate pending invitations are accepted after user signed up (created event)`() =
      validatePendingInvitationsAreAccepted(CREATED)

  // At the moment the user service doesn't send a registered event anymore.
  // However, the event exist in the kafka stream and this test should be use as pre-process test
  @Test
  open fun `validate pending invitations are accepted after user signed up (registered event)`() =
      validatePendingInvitationsAreAccepted(REGISTERED)

  private fun validatePendingInvitationsAreAccepted(eventName: UserEventEnumAvro) {
    useRestoreListener()
    eventStreamGenerator.submitInvitation(asReference = "invitation1Project2") {
      it.lastSent = now().toEpochMilli()
    }
    useOnlineListener()

    eventStreamGenerator.submitUser(asReference = "newUser", eventType = eventName) {
      // The adding of the createdDate is needed for the event of the registered
      // Because in that case no value is set
      it.auditingInformationBuilder.createdDate = timeLineGenerator.next().toEpochMilli()

      it.firstName = "Max"
      it.lastName = "Mustermann"
      it.email = EMAIL_1
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ParticipantEventG3Avro::class.java, ParticipantEventEnumAvro.UPDATED, 2)
        .also {
          validateParticipantUpdatedEvent(it[0].aggregate, participant1Project1)
          validateParticipantUpdatedEvent(it[1].aggregate, participant1Project2)
        }
  }

  private fun validateParticipantUpdatedEvent(
      aggregate: ParticipantAggregateG3Avro,
      participant: Participant
  ) {
    val expectedIdentifier =
        repositories.participantRepository.findById(participant.id!!).get().let {
          AggregateIdentifierAvro.newBuilder()
              .setIdentifier(it.identifier.toString())
              .setVersion(it.version)
              .setType(PARTICIPANT.name)
              .build()
        }
    val user = repositories.userRepository.findOneByEmail(EMAIL_1)!!
    with(aggregate) {
      assertThat(aggregateIdentifier).isEqualByComparingTo(expectedIdentifier)
      assertThat(getUser())
          .isEqualByComparingTo(user.identifier!!.asUserId().toAggregateReference())
      assertThat(status).isEqualTo(VALIDATION)
    }
  }

  companion object {
    private const val EMAIL_1 = "abc@example.com"
    private const val EMAIL_2 = "def@example.com"
  }
}
