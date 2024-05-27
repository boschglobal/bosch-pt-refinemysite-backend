/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.boundary

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SmartSiteSpringBootTest
open class UserTests @Autowired constructor(private var userService: UserService) :
    AbstractEventStreamIntegrationTest() {

  private lateinit var user: UserAggregateAvro

  @BeforeEach
  fun beforeEach() {
    eventStreamGenerator.submitUser(asReference = "csm-user") {
      it.firstName = "Daniel"
      it.lastName = "Düsentrieb"
    }
    user = context["csm-user"] as UserAggregateAvro
  }

  @Test
  fun `check that users are created for user events`() {
    assertThat(userService.findOneByUserId(user.getUserId())).isNotNull
  }

  @Test
  fun `check that existing user is updated for a user updated event`() {
    eventStreamGenerator.submitUser(
        asReference = "csm-user", eventType = UserEventEnumAvro.UPDATED) { it.lastName = "Mayer" }

    val csmUser = userService.findOneByUserId(user.getUserId())
    assertThat(csmUser).isNotNull
  }

  @Test
  fun `check that existing user is deleted for a user deleted event`() {
    eventStreamGenerator.submitUser(asReference = "csm-user", eventType = UserEventEnumAvro.DELETED)

    val csmUser = userService.findOneByUserId(user.getUserId())
    assertThat(csmUser).isNull()
  }

  @Test
  fun `check that existing user is deleted for a user tombstone event`() {
    eventStreamGenerator.submitUserTombstones(reference = "csm-user")

    val csmUser = userService.findOneByUserId(user.getUserId())
    assertThat(csmUser).isNull()
  }

  @Test
  fun `check that nothing happens for a user tombstone event if the user has already been deleted`() {
    val userIdentifier = randomUUID()
    assertDoesNotThrow {
      eventStreamGenerator.submitUserTombstones(
          reference = "csm-user",
          messageKey =
              AggregateEventMessageKey(
                  AggregateIdentifier(USER.value, userIdentifier, 0), userIdentifier))
    }
  }
}
