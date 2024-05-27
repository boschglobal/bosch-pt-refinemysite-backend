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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFileBytes
import com.google.common.collect.Sets.newHashSet
import java.util.TimeZone
import java.util.stream.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Topic Attachment Service")
class TopicAttachmentServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TopicAttachmentService

  private val topicIdentifier by lazy { getIdentifier("topic").asTopicId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitTask().submitTopicG2()
  }

  @ParameterizedTest
  @MethodSource("topicAttachmentSavePermissionGroup")
  @DisplayName("create topic attachment is granted for")
  fun verifySaveTopicAttachmentAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.saveTopicAttachment(
          multiPartFileBytes(), topicIdentifier, "sample.png", null, TimeZone.getDefault())
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName("create topic attachment is denied for non-existing topic for")
  fun verifySaveTopicAttachmentNotAuthorizedForNonExistingTopicFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.saveTopicAttachment(
          multiPartFileBytes(), TopicId(), "sample.jpg", null, TimeZone.getDefault())
    }
  }

  companion object {
    @JvmStatic
    fun topicAttachmentSavePermissionGroup(): Stream<UserTypeAccess> {
      return createGrantedGroup(userTypes, newHashSet(FM_CREATOR))
    }
  }
}
