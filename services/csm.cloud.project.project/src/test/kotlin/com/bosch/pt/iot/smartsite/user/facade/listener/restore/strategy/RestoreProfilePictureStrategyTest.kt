/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePictureTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.user.model.ProfilePicture
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreProfilePictureStrategyTest : AbstractRestoreIntegrationTestV2() {

  private fun getPictureAggregate() = get<UserPictureAggregateAvro>("profilePicture")!!

  private fun getPicture() =
      repositories.profilePictureRepository.findOneWithDetailsByIdentifier(
          getIdentifier("profilePicture"))!!

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().submitUserAndActivate(asReference = "daniel")
  }

  @Test
  open fun `validate profile picture created event`() {
    eventStreamGenerator.submitProfilePicture()

    validatePicture(getPicture(), getPictureAggregate())
  }

  @Test
  open fun `validate profile picture created event if creator doesn't exist anymore`() {
    eventStreamGenerator.submitProfilePicture {
      it.auditingInformationBuilder.createdBy = getUserIdentifierAvro(randomUUID())
    }

    validatePicture(getPicture(), getPictureAggregate())
  }

  @Test
  open fun `validate profile picture updated event`() {
    eventStreamGenerator.submitProfilePicture {
      it.auditingInformationBuilder.createdBy = getUserIdentifierAvro(randomUUID())
      it.smallAvailable = false
    }

    val pictureBefore = getPicture()
    assertThat(pictureBefore.smallAvailable).isFalse
    validatePicture(pictureBefore, getPictureAggregate())

    eventStreamGenerator.submitProfilePicture(eventType = UserPictureEventEnumAvro.UPDATED) {
      // TODO lastModifiedBy instead of createdBy?
      it.auditingInformationBuilder.createdBy = getUserIdentifierAvro(randomUUID())
      it.smallAvailable = true
    }

    val pictureAfter = getPicture()
    assertThat(pictureAfter.smallAvailable).isTrue
    validatePicture(pictureAfter, getPictureAggregate())
  }

  @Test
  open fun `validate profile picture tombstone event`() {
    eventStreamGenerator.submitProfilePicture()

    assertThat(repositories.profilePictureRepository.findAll()).hasSize(1)

    eventStreamGenerator.submitProfilePictureTombstones()

    assertThat(repositories.profilePictureRepository.findAll()).isEmpty()
  }

  private fun validatePicture(picture: ProfilePicture, pictureAggregate: UserPictureAggregateAvro) {
    validateAuditableAndVersionedEntityAttributes(picture, pictureAggregate)

    assertThat(picture.fileSize).isEqualTo(pictureAggregate.getFileSize())
    assertThat(picture.height).isEqualTo(pictureAggregate.getHeight())
    assertThat(picture.width).isEqualTo(pictureAggregate.getWidth())
    assertThat(picture.fullAvailable).isEqualTo(pictureAggregate.getFullAvailable())
    assertThat(picture.smallAvailable).isEqualTo(pictureAggregate.getSmallAvailable())
  }
}
