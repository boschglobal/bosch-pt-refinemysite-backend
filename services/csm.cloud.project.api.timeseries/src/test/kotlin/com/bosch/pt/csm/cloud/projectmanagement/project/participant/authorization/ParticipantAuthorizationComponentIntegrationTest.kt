/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.autorization.ParticipantAuthorizationComponent
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@RmsSpringBootTest
class ParticipantAuthorizationComponentIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var participantAuthorizationComponent: ParticipantAuthorizationComponent

  @Autowired lateinit var participantRepository: ParticipantRepository

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProject("project2")
        .submitParticipantG3("csm-participant2") {
          it.project = getByReference("project2")
          it.user = getByReference("csm-user")
        }

    setAuthentication("csm-user")
  }

  @Test
  fun `verify user has access to all projects`() {
    val participants =
        participantRepository.findAllByUserAndStatus(getIdentifier("csm-user").asUserId(), ACTIVE)

    assertThat(participants).hasSize(2)
    assertThat(participantAuthorizationComponent.hasReadPermissionOnProjects(participants)).isTrue()
  }

  @Test
  fun `verify user has access to a subset of his projects`() {
    val participants =
        participantRepository
            .findAllByUserAndStatus(getIdentifier("csm-user").asUserId(), ACTIVE)
            .toMutableList()
            .also { it.removeAt(1) }

    assertThat(participants).hasSize(1)
    assertThat(participantAuthorizationComponent.hasReadPermissionOnProjects(participants)).isTrue()
  }

  @Test
  fun `verify user has no access to all projects`() {
    eventStreamGenerator.submitUser("user2").submitProject("project3").submitParticipantG3(
        "csm-participant3") {
          it.project = getByReference("project3")
          it.user = getByReference("user2")
        }

    val participantsUser1 =
        participantRepository.findAllByUserAndStatus(getIdentifier("csm-user").asUserId(), ACTIVE)
    assertThat(participantsUser1).hasSize(2)

    val participantsUser2 =
        participantRepository.findAllByUserAndStatus(getIdentifier("user2").asUserId(), ACTIVE)
    assertThat(participantsUser2).hasSize(1)

    val participants = participantsUser1 + participantsUser2
    assertThat(participants).hasSize(3)

    assertThat(participantAuthorizationComponent.hasReadPermissionOnProjects(participants))
        .isFalse()
  }

  @Test
  fun `verify user has no access for empty list of participants`() {
    assertThat(participantAuthorizationComponent.hasReadPermissionOnProjects(emptyList())).isFalse()
  }
}
