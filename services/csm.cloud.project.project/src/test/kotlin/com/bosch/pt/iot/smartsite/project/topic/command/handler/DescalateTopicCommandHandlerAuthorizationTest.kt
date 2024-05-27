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

class DescalateTopicCommandHandlerAuthorizationTest : AbstractTopicAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: EscalateTopicCommandHandler

  private val criticalTopic by lazy {
    repositories.findTopic(getIdentifier("criticalTopic").asTopicId())!!
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify deescalate topic authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(EscalateTopicCommand(criticalTopic.identifier)) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify deescalate topic not authorized for non existing topic`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.handle(EscalateTopicCommand(TopicId())) }
}
