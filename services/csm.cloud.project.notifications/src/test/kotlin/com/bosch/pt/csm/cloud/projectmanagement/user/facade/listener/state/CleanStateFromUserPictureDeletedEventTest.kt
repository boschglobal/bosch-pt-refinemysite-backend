/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.state

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
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
class CleanStateFromUserPictureDeletedEventTest : AbstractEventStreamIntegrationTest() {

  private val user by lazy { context["user"] as UserAggregateAvro }
  private val profilePicture by lazy { context["profilePicture"] as UserPictureAggregateAvro }

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  @Value("\${testadmin.user.identifier}") lateinit var testadminUserIdentifier: String

  val profilePictureIdentifier by lazy { getIdentifier("profilePicture") }

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
        .submitProfilePicture()
  }

  @Test
  fun `when profile picture tombstone message is received`() {
    // Check that profile picture reference exists in database
    assertThat(profilePictureIdentifierOf(user)).isEqualTo(profilePicture.getIdentifier())

    // Delete the profile picture
    eventStreamGenerator.submitProfilePictureTombstones()

    // Check that profile picture reference has been deleted from the database
    assertThat(profilePictureIdentifierOf(user)).isNull()
  }

  @Test
  fun `when profile picture tombstone message is received although profile picture is already deleted`() {
    // Generate the event stream
    eventStreamGenerator.submitProfilePictureTombstones()
    // Check that the profile picture has already been deleted from the database
    assertThat(profilePictureIdentifierOf(user)).isNull()

    // Delete the profile picture
    eventStreamGenerator.submitProfilePictureTombstones(
        messageKey =
            AggregateEventMessageKey(
                AggregateIdentifier(USERPICTURE.value, profilePictureIdentifier, 0),
                profilePictureIdentifier),
    )

    // Check that profile picture reference is still not set
    assertThat(profilePictureIdentifierOf(user)).isNull()
  }

  @Test
  fun `when profile picture deleted event is received`() {

    // Check that profile picture reference exists in database
    assertThat(profilePictureIdentifierOf(user)).isEqualTo(profilePicture.getIdentifier())

    // Delete the profile picture
    eventStreamGenerator.submitProfilePicture(eventType = UserPictureEventEnumAvro.DELETED)

    // Check that profile picture reference has been deleted from the database
    assertThat(profilePictureIdentifierOf(user)).isNull()
  }

  private fun profilePictureIdentifierOf(user: UserAggregateAvro) =
      repositories.userRepository.findById(user.getIdentifier()).get().userPictureIdentifier
}
