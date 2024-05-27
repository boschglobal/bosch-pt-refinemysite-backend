/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.company.facade.listener.online

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.RESENT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.config.MailjetPort
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.mail.template.ParticipantActivatedTemplate
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.util.MailTestHelper
import com.bosch.pt.iot.smartsite.util.cc
import com.bosch.pt.iot.smartsite.util.ccs
import com.bosch.pt.iot.smartsite.util.respondWithSuccess
import com.bosch.pt.iot.smartsite.util.templateId
import java.time.LocalDateTime.now
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * This class tests the activation of participants when a user was assigned to a company. In this
 * case, all participants for that user that are still "in validation" will be activated.
 */
@EnableAllKafkaListeners
open class CompanyEventListenerActivateParticipantsTest : AbstractIntegrationTestV2() {

  companion object {
    const val USER_EMAIL = "abc@test.de"
  }

  @Autowired private lateinit var mailjetPort: MailjetPort
  @Autowired private lateinit var mailTestHelper: MailTestHelper

  private val mockMailjetServer by lazy { MockWebServer().apply { start(mailjetPort.value) } }

  private val invitedParticipantProject1 by lazy {
    repositories.findParticipant(getIdentifier("invitedParticipantProject1"))!!
  }
  private val invitedParticipantProject2 by lazy {
    repositories.findParticipant(getIdentifier("invitedParticipantProject2"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("new-user") { it.email = USER_EMAIL }
        .submitParticipantG3("invitedParticipantProject1") {
          it.company = null
          it.role = FM
          it.status = VALIDATION
        }
        .submitInvitation("invitationProject1") { it.email = USER_EMAIL }
        .submitProject("project2")
        .submitParticipantG3("participantCsmProject2") {
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3("invitedParticipantProject2") {
          it.company = null
          it.role = FM
          it.status = VALIDATION
        }
        .submitInvitation("invitationProject2") { it.email = USER_EMAIL }

    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()

    useOnlineListener()
  }

  @AfterEach
  fun clean() {
    mockMailjetServer.shutdown()
  }

  @Test
  fun `participants in validation are activated when user was assigned to a company`() {
    mockMailjetServer.respondWithSuccess(2)

    eventStreamGenerator.submitEmployee("new-employee")

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, UPDATED, 2)
        .also {
          validateParticipantUpdatedEvent(it[0].aggregate, invitedParticipantProject1)
          validateParticipantUpdatedEvent(it[1].aggregate, invitedParticipantProject2)
        }

    invitationEventStoreUtils.verifyContainsTombstoneMessageAndGet(2).also {
      validateTombstoneMessageKey(it[0], "invitationProject1", 0)
      validateTombstoneMessageKey(it[1], "invitationProject2", 0)
    }

    assertMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, 2)
  }

  @Test
  fun `activate participants in validation when user was assigned to company and inviting participant deactivated`() {
    mockMailjetServer.respondWithSuccess(2)

    eventStreamGenerator
        .submitParticipantG3("participantCsm1", eventType = DEACTIVATED) { it.status = INACTIVE }
        .submitEmployee("new-employee")

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, UPDATED, 2)
        .also {
          validateParticipantUpdatedEvent(it[0].aggregate, invitedParticipantProject1)
          validateParticipantUpdatedEvent(it[1].aggregate, invitedParticipantProject2)
        }

    val userCsm1 = get<UserAggregateAvro>("userCsm1")!!

    assertThatNumberOfMailsWasSent(2)
    assertNextMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, null)
    assertNextMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, userCsm1.email)
  }

  @Test
  fun `participant in validation (without invitation) is activated when user was assigned to a company `() {
    mockMailjetServer.respondWithSuccess(1)

    val emailWithoutInvitation = "no-invitation@example.com"
    eventStreamGenerator
        .submitUser("user-without-invitation") { it.email = emailWithoutInvitation }
        .submitParticipantG3("participant-without-invitation") {
          it.company = null
          it.role = FM
          it.status = VALIDATION
        }

    val participantWithoutInvitation =
        repositories.participantRepository.findOneByIdentifier(
            getIdentifier("participant-without-invitation").asParticipantId())!!

    val invitingUser = eventStreamGenerator.get<UserAggregateAvro>("userCsm1")!!

    eventStreamGenerator.submitEmployee("new-employee") {
      it.user = getByReference("user-without-invitation")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(ParticipantEventG3Avro::class.java, UPDATED, 1)
        .also { validateParticipantUpdatedEvent(it[0].aggregate, participantWithoutInvitation) }

    invitationEventStoreUtils.verifyEmpty()

    assertMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, 1, invitingUser.getEmail())
  }

  @Test
  fun `tomb stone message is produced for resent invitations`() {
    mockMailjetServer.respondWithSuccess(2)

    useRestoreListener()
    eventStreamGenerator.submitInvitation("invitationProject1", eventType = RESENT) {
      it.lastSent = now().toEpochMilli()
    }

    useOnlineListener()
    eventStreamGenerator.submitEmployee("new-employee")

    invitationEventStoreUtils.verifyContainsTombstoneMessageAndGet(3).also {
      validateTombstoneMessageKey(it[0], "invitationProject1", 0)
      validateTombstoneMessageKey(it[1], "invitationProject1", 1)
      validateTombstoneMessageKey(it[2], "invitationProject2", 0)
    }
  }

  @Test
  fun `mail that participant is activated (B2) is sent, one for each inviting project`() {
    mockMailjetServer.respondWithSuccess(2)

    eventStreamGenerator.submitEmployee("new-employee")

    assertMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, 2)
  }

  @Test
  fun `mail that participant is activated (B2) is not sent twice for a resent invitation`() {
    mockMailjetServer.respondWithSuccess(2)

    useRestoreListener()
    eventStreamGenerator.submitInvitation("invitationProject1", eventType = RESENT) {
      it.lastSent = now().toEpochMilli()
    }

    useOnlineListener()
    eventStreamGenerator.submitEmployee("new-employee")

    // still expecting two (and not more) mails to be sent, one for each project
    assertMailSent(ParticipantActivatedTemplate.TEMPLATE_NAME, 2)
  }

  @Test
  fun `all invitations are deleted`() {
    mockMailjetServer.respondWithSuccess(2)

    assertThat(repositories.invitationRepository.count()).isEqualTo(2)

    eventStreamGenerator.submitEmployee("new-employee")

    assertThat(repositories.invitationRepository.count()).isEqualTo(0)
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
    assertThat(aggregate.aggregateIdentifier).isEqualByComparingTo(expectedIdentifier)
    assertThat(aggregate.company).isEqualByComparingTo(getByReference("company"))
    assertThat(aggregate.status).isEqualTo(ACTIVE)
  }

  private fun validateTombstoneMessageKey(
      messageKey: MessageKeyAvro,
      invitationReference: String,
      version: Long
  ) {
    getByReference(invitationReference)
        .also { it.version = version }
        .also { assertThat(messageKey.aggregateIdentifier).isEqualByComparingTo(it) }
  }

  private fun assertMailSent(templateName: String, times: Int, cc: String? = null) {
    assertThat(mockMailjetServer.requestCount).isEqualTo(times)

    (1..times).forEach { _ ->
      val request = mockMailjetServer.takeRequest()
      assertThat(request.templateId()).isIn(mailTestHelper.findAllTemplateIds(templateName))
      if (cc != null) {
        assertThat(request.cc().getString("Email")).isEqualTo(cc)
      }
    }
  }

  private fun assertThatNumberOfMailsWasSent(times: Int) {
    assertThat(mockMailjetServer.requestCount).isEqualTo(times)
  }

  private fun assertNextMailSent(templateName: String, cc: String?) {
    val request = mockMailjetServer.takeRequest()
    assertThat(request.templateId()).isIn(mailTestHelper.findAllTemplateIds(templateName))
    if (cc == null) {
      assertThat(request.ccs().count()).isZero
    } else {
      assertThat(request.cc().getString("Email")).isEqualTo(cc)
    }
  }
}
