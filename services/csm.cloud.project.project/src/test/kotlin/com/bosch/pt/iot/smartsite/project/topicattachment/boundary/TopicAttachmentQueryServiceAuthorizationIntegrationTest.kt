/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Topic Attachment Query Service")
@EnableAllKafkaListeners
class TopicAttachmentQueryServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TopicAttachmentQueryService

  private val topicIdentifier by lazy { getIdentifier("topic").asTopicId() }
  private val topicAttachmentIdentifier by lazy { getIdentifier("topicAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitTask().submitTopicG2().submitTopicAttachment()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  @DisplayName("find a topic attachment is granted for")
  fun verifyFindOneByIdentifierAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findOneByIdentifier(topicAttachmentIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  @DisplayName("find all topic attachments of one topic is granted for")
  fun verifyFindAllByTopicIdentifierAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAllByTopicIdentifier(topicIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  @DisplayName("find all topic and message attachments of one topic is granted for")
  fun verifyFindAllByTopicIdentifierIncludingChildrenAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAllByTopicIdentifierIncludingChildren(topicIdentifier) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName("find one topic attachments is denied for non-existing for")
  fun verifyFindOneByIdentifierNotAuthorizedForNonExistingAttachmentFor(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findOneByIdentifier(randomUUID()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName(
      "find all topic attachments of one topic is denied for non-existing topic attachment for")
  fun verifyFindAllByTopicIdentifierNotAuthorizedForNonExistingTopicAttachmentFor(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAllByTopicIdentifier(TopicId()) }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName(
      "find all topic and message attachments of one topic is denied for non-existing topic attachment for")
  fun verifyFindAllByTopicIdentifierIncludingChildrenNotAuthorizedForNonExistingTopicAttachmentFor(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAllByTopicIdentifierIncludingChildren(TopicId()) }
  }
}
