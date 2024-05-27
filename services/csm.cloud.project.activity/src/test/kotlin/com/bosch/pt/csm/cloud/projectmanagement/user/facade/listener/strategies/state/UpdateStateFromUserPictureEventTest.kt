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
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify user picture state")
@SmartSiteSpringBootTest
class UpdateStateFromUserPictureEventTest : AbstractIntegrationTest() {

  @Test
  fun `is updated after user picture event`() {
    assertThat(findUserPicture()).isNull()
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitProfilePicture { it.user = getByReference("csm-user") }
    }
    assertThat(findUserPicture()).isNotNull
  }

  private fun findUserPicture() =
      repositories.userRepository.findOneCachedByIdentifier(csmUser.getIdentifier())
          ?.userPictureIdentifier
}
