/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.job

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.config.MailjetPort
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateScheduledJob
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.testdata.invitationForUnregisteredUser
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import com.bosch.pt.iot.smartsite.util.respondWithSuccess
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@EnableAllKafkaListeners
@TestPropertySource(properties = ["custom.mail.job.resend-invitation.enabled=true"])
class InvitationReminderJobTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var mailjetPort: MailjetPort
  @Autowired lateinit var cut: InvitationReminderJob

  lateinit var mockServer: MockWebServer

  @BeforeEach
  fun setupTestData() {

    mockServer =
        MockWebServer().apply {
          respondWithSuccess()
          start(mailjetPort.value)
        }

    timeLineGenerator.resetAtDaysAgo(28)
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .plainProjectWithCsm()
        .invitationForUnregisteredUser(referencePrefix = "invited-user-1")

    timeLineGenerator.resetAtDaysAgo(22)
    eventStreamGenerator.invitationForUnregisteredUser(referencePrefix = "invited-user-2")
    timeLineGenerator.resetAtDaysAgo(21)
    eventStreamGenerator.invitationForUnregisteredUser(referencePrefix = "invited-user-3")
    timeLineGenerator.resetAtDaysAgo(20)
    eventStreamGenerator.invitationForUnregisteredUser(referencePrefix = "invited-user-4")

    timeLineGenerator.resetAtDaysAgo(8)
    eventStreamGenerator.invitationForUnregisteredUser(referencePrefix = "invited-user-5")
    timeLineGenerator.resetAtDaysAgo(7)
    eventStreamGenerator
        .invitationForUnregisteredUser(referencePrefix = "invited-user-6")
        .invitationForUnregisteredUser(referencePrefix = "invited-user-7")
        .submitUser(asReference = "user-7") {
          it.email = get<InvitationAggregateAvro>("invited-user-7-invitation")!!.getEmail()
        }
        .submitParticipantG3(asReference = "invited-user-7-participant", eventType = UPDATED) {
          it.user = getByReference("user-7")
          it.status = VALIDATION
        }
    timeLineGenerator.resetAtDaysAgo(6)
    eventStreamGenerator.invitationForUnregisteredUser(referencePrefix = "invited-user-8")

    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
  }

  @AfterEach
  fun tearDown() {
    mockServer.shutdown()
  }

  @Test
  @DisplayName("Send (first) reminder for invitations created 7 days ago")
  fun sendFirstReminder() {

    val invitation =
        eventStreamGenerator.get<InvitationAggregateAvro>("invited-user-6-invitation")!!

    simulateScheduledJob { cut.sendFirstReminders() }

    invitationEventStoreUtils
        .verifyContainsAndGet(InvitationEventAvro::class.java, InvitationEventEnumAvro.RESENT, 1)
        .first()
        .getAggregate()
        .also {
          assertThat(it.getParticipantIdentifier()).isEqualTo(invitation.getParticipantIdentifier())
        }
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("Send (second) reminder for invitations created 21 days ago")
  fun sendSecondReminder() {

    val invitation =
        eventStreamGenerator.get<InvitationAggregateAvro>("invited-user-3-invitation")!!

    simulateScheduledJob { cut.sendSecondReminders() }

    invitationEventStoreUtils
        .verifyContainsAndGet(InvitationEventAvro::class.java, InvitationEventEnumAvro.RESENT, 1)
        .first()
        .getAggregate()
        .also {
          assertThat(it.getParticipantIdentifier()).isEqualTo(invitation.getParticipantIdentifier())
        }
    projectEventStoreUtils.verifyEmpty()
  }
}
