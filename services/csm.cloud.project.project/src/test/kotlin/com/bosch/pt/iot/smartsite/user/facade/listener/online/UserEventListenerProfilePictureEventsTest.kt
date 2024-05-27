/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.online

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getVersion
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EnableAllKafkaListeners
open class UserEventListenerProfilePictureEventsTest : AbstractIntegrationTestV2() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitSystemUserAndActivate().submitUser(asReference = "daniel") {
      it.firstName = "Daniel"
      it.lastName = "Düsentrieb"
      it.crafts = emptyList()
      it.email = "daniel@smartsite.com"
      it.registered = false
      it.phoneNumbers = emptyList()
    }
  }

  @Test
  fun `validate profile picture created event`() {
    eventStreamGenerator.submitProfilePicture()

    val userAggregate: UserAggregateAvro = get("daniel")!!
    val userPictureAggregate: UserPictureAggregateAvro = get("profilePicture")!!

    repositories.profilePictureRepository
        .findOneByUserIdentifier(userAggregate.getIdentifier())!!.apply {
          assertThat(fileSize).isEqualTo(userPictureAggregate.getFileSize())
          assertThat(height).isEqualTo(userPictureAggregate.getHeight())
          assertThat(width).isEqualTo(userPictureAggregate.getWidth())
          assertThat(fullAvailable).isEqualTo(userPictureAggregate.getFullAvailable())
          assertThat(smallAvailable).isEqualTo(userPictureAggregate.getSmallAvailable())
          assertThat(version).isEqualTo(userPictureAggregate.getAggregateIdentifier().getVersion())
        }
  }

  @Test
  fun `validate profile picture updated event`() {

    eventStreamGenerator.submitProfilePicture { it.smallAvailable = false }

    val userAggregate: UserAggregateAvro = get("daniel")!!

    repositories.profilePictureRepository
        .findOneByUserIdentifier(userAggregate.getIdentifier())!!.apply {
          assertThat(smallAvailable).isFalse()
        }

    eventStreamGenerator.submitProfilePicture(eventType = UPDATED) { it.smallAvailable = true }

    val userPictureAggregate: UserPictureAggregateAvro = get("profilePicture")!!

    repositories.profilePictureRepository
        .findOneByUserIdentifier(userAggregate.getIdentifier())!!.apply {
          assertThat(smallAvailable).isTrue
          assertThat(version).isEqualTo(userPictureAggregate.getVersion())
        }
  }

  @Test
  fun `validate profile picture tombstone event`() {
    eventStreamGenerator
        .submitProfilePicture { it.smallAvailable = false }
        .submitProfilePictureTombstones()

    val userAggregate: UserAggregateAvro = get("daniel")!!

    repositories.profilePictureRepository
        .findOneByUserIdentifier(userAggregate.getAggregateIdentifier().getIdentifier().toUUID())
        .apply { assertThat(this).isNull() }
  }
}
