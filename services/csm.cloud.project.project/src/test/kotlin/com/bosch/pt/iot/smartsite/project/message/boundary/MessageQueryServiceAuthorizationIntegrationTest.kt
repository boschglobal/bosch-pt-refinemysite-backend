/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class MessageQueryServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: MessageQueryService

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }
  private val message by lazy { repositories.findMessage(getIdentifier("message").asMessageId())!! }

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitTopicG2()
        .submitMessage()
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find message by identifier authorized for`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findOneByIdentifier(message.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find one permission is denied for non-existing message`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findOneByIdentifier(MessageId()) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find paged permission is granted to`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.findPagedMessageByTopicIdAndMessageDate(topic.identifier, null, 3)
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find paged permission is denied for non-existing message`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.findPagedMessageByTopicIdAndMessageDate(TopicId(), null, 3) }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find messages by task identifier permission is granted to`(userType: UserTypeAccess) =
      checkAccessWith(userType) {
        cut.findByTaskIdentifiers(listOf(task.identifier), PageRequest.of(0, 1))
      }

  @ParameterizedTest
  @MethodSource("noOneWithAccess")
  fun `verify find messages by task identifier permission is denied for task without view access`(
      userType: UserTypeAccess
  ) =
      checkAccessWith(userType) {
        cut.findByTaskIdentifiers(listOf(task.identifier, TaskId()), PageRequest.of(0, 1))
      }
}
