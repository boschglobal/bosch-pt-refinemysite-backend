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
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFileBytes
import com.google.common.collect.Sets.newHashSet
import java.util.TimeZone
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Test authorization in Message Attachment Service")
class MessageAttachmentServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: MessageAttachmentService

  private val messageIdentifier by lazy { getIdentifier("message").asMessageId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProjectCraftG2().submitTask().submitTopicG2().submitMessage()
  }

  @ParameterizedTest
  @MethodSource("messageAttachmentSavePermissionGroup")
  @DisplayName("save message attachment is granted for")
  fun verifySaveMessageAttachmentAuthorizedFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val attachmentIdentifier =
          cut.saveMessageAttachment(
              multiPartFileBytes(), messageIdentifier, "sample.png", null, TimeZone.getDefault())
      assertThat(attachmentIdentifier).isNotNull()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  @DisplayName("save message attachment is denied for non-existing message for")
  fun verifySaveMessageAttachmentNotAuthorizedNonExistingMessageFor(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.saveMessageAttachment(
          multiPartFileBytes(), MessageId(), "sample.png", null, TimeZone.getDefault())
    }
  }

  companion object {
    @JvmStatic
    fun messageAttachmentSavePermissionGroup(): Stream<UserTypeAccess> {
      return createGrantedGroup(userTypes, newHashSet(FM_CREATOR))
    }
  }
}
