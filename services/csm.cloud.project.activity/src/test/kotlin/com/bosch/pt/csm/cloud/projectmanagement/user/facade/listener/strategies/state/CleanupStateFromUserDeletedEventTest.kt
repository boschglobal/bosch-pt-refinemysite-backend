/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify user state")
@SmartSiteSpringBootTest
class CleanupStateFromUserDeletedEventTest : AbstractIntegrationTest() {

  @Test
  fun `when user tombstone message is received`() {
    assertThat(findUser()).isNotNull
    eventStreamGenerator.submitUserTombstones(reference = "fm-user")
    assertThat(findUser()).isNull()
  }

  @Test
  fun `when user tombstone message is received although user is already deleted`() {
    assertThat(findUser()).isNotNull
    eventStreamGenerator.submitUserTombstones(reference = "fm-user")
    assertThat(findUser()).isNull()
    eventStreamGenerator.submitUserTombstones(reference = "fm-user")
    assertThat(findUser()).isNull()
  }

  @Test
  fun `is cleaned up after user deleted event`() {
    assertThat(findUser()).isNotNull
    eventStreamGenerator.submitUser(asReference = "fm-user", eventType = DELETED)
    assertThat(findUser()).isNull()
  }

  private fun findUser() =
      repositories.userRepository.findOneCachedByIdentifier(fmUser.getIdentifier())
}
