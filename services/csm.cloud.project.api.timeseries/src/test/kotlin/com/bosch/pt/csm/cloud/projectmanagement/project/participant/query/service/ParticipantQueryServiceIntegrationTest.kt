/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@RmsSpringBootTest
class ParticipantQueryServiceIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var participantQueryService: ParticipantQueryService

  @SpykBean lateinit var participantRepository: ParticipantRepository

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProject("project2")
        .submitParticipantG3("csm-participant2") {
          it.project = EventStreamGeneratorStaticExtensions.getByReference("project2")
          it.user = EventStreamGeneratorStaticExtensions.getByReference("csm-user")
        }
        .submitUser("user2")
        .submitProject("project3")
        .submitParticipantG3("csm-participant3") {
          it.project = EventStreamGeneratorStaticExtensions.getByReference("project3")
          it.user = EventStreamGeneratorStaticExtensions.getByReference("user2")
        }

    setAuthentication("csm-user")
  }

  @AfterEach
  fun cleanup() {
    clearMocks(participantRepository)
  }

  @Test
  fun `verify that access is allowed`() {
    assertThat(participantQueryService.findActiveParticipantsOfCurrentUser()).isNotEmpty
  }

  @Test
  fun `verify that PostAuthorize works as expected`() {
    val participantsUser1 =
        participantRepository.findAllByUserAndStatus(
            EventStreamGeneratorStaticExtensions.getIdentifier("csm-user").asUserId(), ACTIVE)
    assertThat(participantsUser1).hasSize(2)

    val participantsUser2 =
        participantRepository.findAllByUserAndStatus(
            EventStreamGeneratorStaticExtensions.getIdentifier("user2").asUserId(), ACTIVE)
    assertThat(participantsUser2).hasSize(1)

    val participants = participantsUser1 + participantsUser2
    assertThat(participants).hasSize(3)

    every { participantRepository.findAllByUserAndStatus(any(), ACTIVE) } returns
        participants andThen
        participantsUser1

    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      participantQueryService.findActiveParticipantsOfCurrentUser()
    }
  }
}
