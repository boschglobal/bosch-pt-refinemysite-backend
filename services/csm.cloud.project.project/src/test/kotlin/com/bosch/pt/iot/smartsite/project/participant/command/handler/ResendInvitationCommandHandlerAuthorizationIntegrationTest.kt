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
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.api.ResendInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.sideeffects.ParticipantMailService
import com.bosch.pt.iot.smartsite.testdata.invitationForUnregisteredUser
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for ResendInvitationCommandHandler")
class ResendInvitationCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var participantMailService: ParticipantMailService

  @Autowired private lateinit var cut: ResendInvitationCommandHandler

  @ParameterizedTest
  @DisplayName("resent invitation is granted to")
  @MethodSource("csmWithAccess")
  fun verifyResendInvitationAuthorized(userType: UserTypeAccess) {
    eventStreamGenerator.invitationForUnregisteredUser()

    checkAccessWith(userType) {
      cut.handle(
          ResendInvitationCommand(getIdentifier("unregistered-user-participant").asParticipantId()))
    }
  }

  @ParameterizedTest
  @DisplayName("resent invitation is denied for unknown participant")
  @MethodSource("noOneWithAccess")
  fun verifyResendInvitationNotAuthorizedForUnknownParticipant(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.handle(ResendInvitationCommand(ParticipantId())) }
  }
}
