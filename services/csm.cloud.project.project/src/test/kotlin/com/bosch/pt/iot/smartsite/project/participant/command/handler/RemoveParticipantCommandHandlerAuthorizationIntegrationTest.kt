/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.RemoveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for DeactivateParticipantCommandHandler")
class RemoveParticipantCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: RemoveParticipantCommandHandler

  @ParameterizedTest
  @DisplayName("delete project participant")
  @MethodSource("csmWithAccess")
  fun verifyDeleteParticipantAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(RemoveParticipantCommand(getIdentifier("participantFm").asParticipantId()))
    }
  }

  @ParameterizedTest
  @DisplayName("delete project participant CR")
  @MethodSource("csmWithAccess")
  fun verifyDeleteParticipantCrAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(RemoveParticipantCommand(getIdentifier("participantCr").asParticipantId()))
    }
  }

  @ParameterizedTest
  @DisplayName("delete project participant is denied for unknown participant")
  @MethodSource("noOneWithAccess")
  fun verifyDeleteParticipantNotAuthorizedForUnknownParticipant(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.handle(RemoveParticipantCommand(ParticipantId())) }
  }
}
