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
import com.bosch.pt.iot.smartsite.project.participant.command.api.UpdateParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization for UpdateParticipantCommandHandler")
class UpdateParticipantCommandHandlerAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: UpdateParticipantCommandHandler

  @ParameterizedTest
  @DisplayName("update project participant is granted to")
  @MethodSource("csmWithAccess")
  fun verifyUpdateParticipantAuthorized(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          UpdateParticipantCommand(
              getIdentifier("participantFm").asParticipantId(), 0, ParticipantRoleEnum.CR))
    }
  }

  @ParameterizedTest
  @DisplayName("update project participant is denied to unknown participant")
  @MethodSource("noOneWithAccess")
  fun verifyUpdateParticipantNotAuthorizedForNullParticipant(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(UpdateParticipantCommand(ParticipantId(), 0, ParticipantRoleEnum.CR))
    }
  }
}
