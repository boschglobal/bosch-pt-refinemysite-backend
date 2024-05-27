/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.query

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("Test authorization for Project Participant Service")
class ParticipantQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: ParticipantQueryService

  private val participantRoles = setOf(*ParticipantRoleEnum.values())

  @ParameterizedTest
  @DisplayName("find single project participant")
  @MethodSource("allActiveParticipantsWithAccess")
  fun verifyFindParticipantWithDetailsAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val participant =
          cut.findParticipantWithDetails(getIdentifier("participantFm").asParticipantId())
      assertThat(participant).isNotNull()
    }
  }

  @ParameterizedTest
  @DisplayName("find assignable project participants")
  @MethodSource("allActiveParticipantsWithAccess")
  fun verifyFindAssignableParticipantsAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.findAllAssignableParticipants(project.identifier, null, PageRequest.of(0, 10))
    }
  }

  @ParameterizedTest
  @DisplayName("find project participant companies")
  @MethodSource("allActiveParticipantsWithAccess")
  fun verifyFindParticipantCompaniesAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.findAllAssignableCompanies(project.identifier, false, PageRequest.of(0, 10))
    }
  }

  @ParameterizedTest
  @DisplayName("find project participants")
  @MethodSource("allActiveParticipantsWithAccess")
  fun verifyFindParticipantsAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.findAllParticipants(
          project.identifier, null, null, participantRoles, PageRequest.of(0, 10))
    }
  }
}
