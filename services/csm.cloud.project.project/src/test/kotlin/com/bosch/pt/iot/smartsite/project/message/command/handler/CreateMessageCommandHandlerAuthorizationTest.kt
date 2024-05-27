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
import com.bosch.pt.iot.smartsite.project.message.command.api.CreateMessageCommand
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateMessageCommandHandlerAuthorizationTest : AbstractMessageAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreateMessageCommandHandler

  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify create message authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            CreateMessageCommand(
                identifier = MessageId(),
                content = "content",
                topicIdentifier = topic.identifier,
                projectIdentifier = project.identifier))
      }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify create message not authorized for non-existing topic`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.handle(
            CreateMessageCommand(
                identifier = MessageId(),
                content = "content",
                topicIdentifier = TopicId(),
                projectIdentifier = project.identifier))
      }
}
