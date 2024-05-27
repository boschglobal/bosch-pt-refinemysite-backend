/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.job

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateScheduledJob
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.testdata.invitationForUnregisteredUser
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@EnableAllKafkaListeners
@TestPropertySource(properties = ["custom.mail.job.expire-invitations.enabled=true"])
class InvitationExpirationJobTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: InvitationExpirationJob

  @BeforeEach
  fun setUp() {
    timeLineGenerator.resetAtDaysAgo(32)
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
  }

  @Test
  fun `Delete invitations when last sent timestamp has expired by 31 days`() {
    eventStreamGenerator.invitationForUnregisteredUser()

    simulateScheduledJob { cut.cleanUpExpiredInvitations() }

    getIdentifier("unregistered-user-participant")
        .asParticipantId()
        .let { repositories.participantRepository.findOneByIdentifier(it) }
        .apply { assertThat(this).isNull() }

    repositories.invitationRepository.findAll().apply { assertThat(this).isEmpty() }
  }

  @Test
  fun `Delete nothing when there are no expired invitations`() {
    timeLineGenerator.resetAtDaysAgo(28)
    eventStreamGenerator.invitationForUnregisteredUser()

    simulateScheduledJob { cut.cleanUpExpiredInvitations() }

    getIdentifier("unregistered-user-participant")
        .asParticipantId()
        .let { repositories.participantRepository.findOneByIdentifier(it) }
        .apply { assertThat(this).isNotNull }

    repositories.invitationRepository.findAll().apply { assertThat(this).isNotEmpty() }
  }

  // Tests bug fix for SMAR-13109: NullPointerException in InvitationExpirationJob when invited
  // participant was deleted
  @Test
  fun `expiration succeeds if invited participant was deleted`() {
    eventStreamGenerator.invitationForUnregisteredUser()

    val invitedParticipant =
        repositories.participantRepository.findOneByIdentifier(
            getIdentifier("unregistered-user-participant").asParticipantId())!!

    // delete invited participant before expiring the invitation
    val csmUser = repositories.userRepository.findOneByIdentifier(getIdentifier("csm-user"))
    doWithAuthorization(csmUser) {
      transactionTemplate.execute { repositories.participantRepository.delete(invitedParticipant) }
    }

    repositories.participantRepository.findOneByIdentifier(invitedParticipant.identifier).apply {
      assertThat(this).isNull()
    }

    simulateScheduledJob { cut.cleanUpExpiredInvitations() }

    repositories.apply { assertThat(invitationRepository.findAll()).isEmpty() }
  }
}
