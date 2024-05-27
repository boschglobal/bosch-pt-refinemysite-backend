/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.message.command.api.DeleteMessageCommand
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class DeleteMessageCommandHandlerAuthorizationTest : AbstractMessageAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: DeleteMessageCommandHandler

  private val message by lazy { repositories.findMessage(getIdentifier("message").asMessageId())!! }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify delete message authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(DeleteMessageCommand(message.identifier)) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify delete message not authorized for non-existing message`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(DeleteMessageCommand(MessageId())) }
}
