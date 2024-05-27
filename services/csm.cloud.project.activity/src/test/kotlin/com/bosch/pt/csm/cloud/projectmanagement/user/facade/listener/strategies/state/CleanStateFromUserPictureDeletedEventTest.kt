/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify user picture state")
@SmartSiteSpringBootTest
class CleanStateFromUserPictureDeletedEventTest : AbstractIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setLastIdentifierForType(USER.value, getByReference("csm-user"))
        .submitProfilePicture()
  }

  @Test
  fun `when user tombstone message is received`() {
    assertThat(findUserPicture()).isNotNull
    eventStreamGenerator.submitProfilePictureTombstones(reference = "profilePicture")
    assertThat(findUserPicture()).isNull()
  }

  @Test
  fun `when user tombstone message is received although user picture is already deleted`() {
    assertThat(findUserPicture()).isNotNull

    eventStreamGenerator.submitProfilePicture(eventType = DELETED)
    assertThat(findUserPicture()).isNull()

    eventStreamGenerator.submitProfilePictureTombstones(reference = "profilePicture")
    assertThat(findUserPicture()).isNull()
  }

  @Test
  fun `is cleaned up after user picture deleted event`() {
    assertThat(findUserPicture()).isNotNull
    eventStreamGenerator.submitProfilePicture(eventType = DELETED)
    assertThat(findUserPicture()).isNull()
  }

  private fun findUserPicture() =
      repositories.userRepository.findOneCachedByIdentifier(csmUser.getIdentifier())
          ?.userPictureIdentifier
}
