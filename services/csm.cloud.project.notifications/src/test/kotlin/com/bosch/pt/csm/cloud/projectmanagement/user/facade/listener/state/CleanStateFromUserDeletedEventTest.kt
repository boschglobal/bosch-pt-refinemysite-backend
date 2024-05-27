/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.state

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
@DisplayName("State must be cleaned up ... ")
@SmartSiteSpringBootTest
class CleanStateFromUserDeletedEventTest : AbstractEventStreamIntegrationTest() {

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  @Value("\${testadmin.user.identifier}") lateinit var testadminUserIdentifier: String

  val userIdentifier by lazy { getIdentifier("user") }

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitUserAndActivate(asReference = "testadmin") {
          it.aggregateIdentifierBuilder.identifier = testadminUserIdentifier
          it.userId = testadminUserId
        }
        .submitUser(asReference = "user") {
          it.firstName = "Ali"
          it.lastName = "Albatros"
        }
  }
  @Test
  fun `when user tombstone message is received`() {
    // Check that user exists in database
    assertThat(findUserBy(userIdentifier)).isNotNull

    AggregateIdentifierAvro()

    // Delete the user
    eventStreamGenerator.submitUserTombstones(
        reference = "user",
        messageKey =
            AggregateEventMessageKey(
                AggregateIdentifier(USER.value, userIdentifier, 0), userIdentifier))

    // Check that the user has been deleted from the database
    assertThat(findUserBy(userIdentifier)).isNull()
  }

  @Test
  fun `when user tombstone message is received although user is already deleted`() {

    // Generate event stream
    eventStreamGenerator.submitUser(asReference = "user", eventType = UserEventEnumAvro.DELETED)

    // Check that the user has already been deleted from the database
    assertThat(findUserBy(userIdentifier)).isNull()

    eventStreamGenerator.submitUserTombstones(
        reference = "user",
        messageKey =
            AggregateEventMessageKey(
                AggregateIdentifier(USER.value, userIdentifier, 0), userIdentifier))

    // Check that the user has been deleted from the database
    assertThat(findUserBy(userIdentifier)).isNull()
  }

  @Test
  fun `when user deleted event is received`() {
    // Check that user exists in database
    assertThat(findUserBy(userIdentifier)).isNotNull

    // Delete the user
    eventStreamGenerator.submitUser(asReference = "user", eventType = UserEventEnumAvro.DELETED)

    // Check that the user has been deleted from the database
    assertThat(findUserBy(userIdentifier)).isNull()
  }

  private fun findUserBy(identifier: UUID): User? =
      repositories.userRepository.findById(identifier).orElse(null)
}
