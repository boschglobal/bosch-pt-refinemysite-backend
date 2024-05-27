/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Message Attachment Query Service")
@EnableAllKafkaListeners
class MessageAttachmentQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: MessageAttachmentQueryService

  private val messageIdentifier by lazy { getIdentifier("message").asMessageId() }
  private val messageAttachmentIdentifier by lazy { getIdentifier("messageAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProjectCraftG2()
        .submitTask()
        .submitTopicG2()
        .submitMessage()
        .submitMessageAttachment()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  @DisplayName("find single message attachment is granted for")
  fun verifyFindOneByIdentifierAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findOneByIdentifier(messageAttachmentIdentifier)).isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  @DisplayName("find all message attachments for message identifier is granted for")
  fun verifyFindAllByMessageIdentifierAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findAllByMessageIdentifier(messageIdentifier)).hasSize(1)
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  @DisplayName("find single message attachment is denied for non-existing attachment for")
  fun verifyFindOneByIdentifierAuthorizedNotAuthorizedNonExistingAttachmentFor(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findOneByIdentifier(randomUUID()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  @DisplayName(
      "find all message attachments for message id permission is denied for non-existing message for")
  fun verifyFindAllByMessageIdentifierNotAuthorizedNonExistingMessageFor(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAllByMessageIdentifier(MessageId()) }
  }
}
