/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore

import com.bosch.pt.csm.cloud.common.CodeExample
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractRestoreIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.picture.toProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

@CodeExample
class RestoreProfilePictureSnapshotTest : AbstractRestoreIntegrationTest() {

  @Test
  fun `validate profile picture created event`() {
    eventStreamGenerator.submitUserAndActivate("user").submitProfilePicture()

    val pictureAggregate = eventStreamGenerator.get<UserPictureAggregateAvro>("profilePicture")!!

    repositories.profilePictureRepository
        .findOneWithDetailsByIdentifier(pictureAggregate.toProfilePictureId())!!.also {
          validatePictureAttributes(it, pictureAggregate)
          validateAuditingInformation(it, pictureAggregate)
        }
  }

  @Test
  fun `missing tombstone message does not cause unique constraint violation`() {
    /**
     * When a user profile picture is changed, the User service emits a tombstone event for the old
     * picture, then a CREATED event for the new one. Due to a bug, the tombstone message may be
     * missing. During restore, the second CREATED event would cause a unique constraint violation
     * on inserting into the user picture table. As a workaround, we explicitly delete old pictures
     * from the table before an insert.
     */
    eventStreamGenerator
        .submitUserAndActivate("user")
        .submitProfilePicture("picture1") { it.smallAvailable = false }
        .submitProfilePicture("picture1", eventType = UPDATED) { it.smallAvailable = true }
        .submitProfilePicture("picture2") { it.smallAvailable = false }

    fun profilePicture(ref: String) =
        repositories.profilePictureRepository.findOneWithDetailsByIdentifier(
            eventStreamGenerator.get<UserPictureAggregateAvro>(ref)!!.toProfilePictureId())

    assertThat(profilePicture("picture1")).isNull()
    assertThat(profilePicture("picture2")).isNotNull
  }

  @Test
  fun `validate profile picture updated event`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture { it.smallAvailable = false }

    var pictureAggregate = eventStreamGenerator.get<UserPictureAggregateAvro>("profilePicture")!!

    repositories.profilePictureRepository
        .findOneWithDetailsByIdentifier(pictureAggregate.toProfilePictureId())!!.also {
          assertThat(it.isSmallAvailable()).isFalse
          validatePictureAttributes(it, pictureAggregate)
          validateAuditingInformation(it, pictureAggregate)
        }

    eventStreamGenerator.submitProfilePicture(eventType = UPDATED) { it.smallAvailable = true }

    pictureAggregate = eventStreamGenerator.get<UserPictureAggregateAvro>("profilePicture")!!

    repositories.profilePictureRepository
        .findOneWithDetailsByIdentifier(pictureAggregate.toProfilePictureId())!!.also {
          assertThat(it.isSmallAvailable()).isTrue
          validatePictureAttributes(it, pictureAggregate)
          validateAuditingInformation(it, pictureAggregate)
        }
  }

  @Test
  fun `validate profile picture tombstone event`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture()

    repositories.profilePictureRepository.findAll().also { assertThat(it).hasSize(1) }

    eventStreamGenerator.submitProfilePictureTombstones()

    repositories.profilePictureRepository.findAll().also { assertThat(it).hasSize(0) }
  }

  @Test
  fun `validate profile picture deleted event is no longer supported`() {
    eventStreamGenerator.submitUser("user").submitProfilePicture()

    assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
      eventStreamGenerator.submitProfilePicture(eventType = DELETED)
    }
  }

  private fun validatePictureAttributes(
      picture: ProfilePicture,
      pictureAggregate: UserPictureAggregateAvro
  ) {
    picture.apply {
      assertThat(getIdentifierUuid()).isEqualTo(pictureAggregate.getIdentifier())
      assertThat(version).isEqualTo(pictureAggregate.aggregateIdentifier.version)
      assertThat(fileSize).isEqualTo(pictureAggregate.fileSize)
      assertThat(height).isEqualTo(pictureAggregate.height)
      assertThat(width).isEqualTo(pictureAggregate.width)
      assertThat(isFullAvailable()).isEqualTo(pictureAggregate.fullAvailable)
      assertThat(isSmallAvailable()).isEqualTo(pictureAggregate.smallAvailable)
    }
  }
}
