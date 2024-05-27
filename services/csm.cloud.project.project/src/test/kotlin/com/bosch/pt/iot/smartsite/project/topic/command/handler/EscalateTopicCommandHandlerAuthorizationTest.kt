/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.topic.command.api.EscalateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class EscalateTopicCommandHandlerAuthorizationTest : AbstractTopicAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: EscalateTopicCommandHandler

  private val uncriticalTopic by lazy {
    repositories.findTopic(getIdentifier("uncriticalTopic").asTopicId())!!
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify escalate topic authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(EscalateTopicCommand(uncriticalTopic.identifier)) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify escalate topic not authorized for non existing topic`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(EscalateTopicCommand(TopicId())) }
}
