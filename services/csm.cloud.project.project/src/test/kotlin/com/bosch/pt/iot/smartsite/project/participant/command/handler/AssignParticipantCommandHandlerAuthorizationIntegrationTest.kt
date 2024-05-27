/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

@DisplayName("Test authorization for AssignParticipantCommandHandler")
class AssignParticipantCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Suppress("Unused", "UnusedPrivateMember")
  @MockBean
  private lateinit var participantMailService: ParticipantMailService

  @Autowired private lateinit var cut: AssignParticipantCommandHandler

  @ParameterizedTest
  @DisplayName("assign project participant is granted to")
  @MethodSource("csmWithAccess")
  fun verifyAssignParticipantAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          AssignParticipantCommand(
              ParticipantId(),
              project.identifier,
              userNotParticipant.email!!,
              ParticipantRoleEnum.CR))
    }
  }

  @ParameterizedTest
  @DisplayName("assign project participant is denied to unknown project")
  @MethodSource("noOneWithAccess")
  fun verifyAssignParticipantNotAuthorizedForRandomUuid(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          AssignParticipantCommand(
              ParticipantId(), ProjectId(), userNotParticipant.email!!, ParticipantRoleEnum.CR))
    }
  }
}
