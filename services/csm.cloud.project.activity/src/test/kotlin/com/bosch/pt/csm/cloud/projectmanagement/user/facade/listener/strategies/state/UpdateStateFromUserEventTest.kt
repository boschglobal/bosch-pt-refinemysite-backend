/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.user.model.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.FEMALE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify user state")
@SmartSiteSpringBootTest
class UpdateStateFromUserEventTest : AbstractIntegrationTest() {

  @Test
  fun `is updated after user event without gender and locale`() {
    val count = repositories.userRepository.findAll().count()

    eventStreamGenerator.setUserContext("testadmin").repeat {
      eventStreamGenerator
          .submitUser("new-user") {
            it.firstName = "first"
            it.lastName = "last"
            it.gender = null
            it.locale = null
          }
          .submitUser("new-user", eventType = UPDATED) { it.firstName = "update first" }
    }

    assertThat(repositories.userRepository.findAll()).hasSize(count + 1)
    val user = repositories.userRepository.findOneCachedByIdentifier(getIdentifier("new-user"))
    assertThat(user?.displayName).isEqualTo("update first last")
    assertThat(user?.gender).isNull()
    assertThat(user?.getUserLocale()).isNull()
  }

  @Test
  fun `is updated after user event with gender and locale`() {
    eventStreamGenerator.setUserContext("testadmin").submitUser("new-user") {
      it.firstName = "first"
      it.lastName = "last"
      it.gender = FEMALE
      it.locale = Locale.GERMANY.toString()
    }

    val user = repositories.userRepository.findOneCachedByIdentifier(getIdentifier("new-user"))
    assertThat(user?.gender).isEqualTo(GenderEnum.FEMALE)
    assertThat(user?.getUserLocale()).isEqualTo(Locale.GERMANY)
  }
}
