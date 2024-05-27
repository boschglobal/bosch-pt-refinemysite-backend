/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.picture.facade.rest

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro.UPDATED
import org.apache.tika.Tika
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus.CREATED
import org.springframework.mock.web.MockMultipartFile

class ProfilePictureApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: ProfilePictureController

  @Value("classpath:img/gps.jpg") private lateinit var image: Resource

  @Test
  fun `upload profile picture with valid content type`() {
    eventStreamGenerator.submitUser("user")
    setAuthentication("user")

    val userId = eventStreamGenerator.getIdentifier("user").asUserId()
    val user = repositories.userRepository.findOneByIdentifier(userId)!!

    val picture =
        MockMultipartFile(
            image.filename!!, null, Tika().detect(image.file), image.contentAsByteArray)
    val response = cut.saveProfilePicture(picture, userId, null, user)
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body!!.fileSize).isEqualTo(picture.size)
    assertThat(response.body!!.version).isEqualTo(0)
    assertThat(response.body!!.userReference.identifier).isEqualTo(userId.identifier)
    // Expect that exif data is not available after upload
    assertThat(response.body!!.height).isEqualTo(0)
    assertThat(response.body!!.width).isEqualTo(0)
  }

  @Test
  fun `upload profile picture with invalid content type`() {
    eventStreamGenerator.submitUser("user")
    setAuthentication("user")

    val userId = eventStreamGenerator.getIdentifier("user").asUserId()
    val user = repositories.userRepository.findOneByIdentifier(userId)!!

    val contentType = "invalid-content-type"
    val picture = MockMultipartFile("file.jpg", null, contentType, "data".toByteArray())

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.saveProfilePicture(picture, userId, null, user) }
        .withMessage(IMAGE_VALIDATION_ERROR_UNSUPPORTED_IMAGE_TYPE)
  }

  @Test
  fun `default profile picture is returned for a user without a profile picture`() {
    eventStreamGenerator.submitUser("user")

    setAuthentication("user")

    val userId = eventStreamGenerator.getIdentifier("user").asUserId()
    val user = repositories.userRepository.findOneByIdentifier(userId)!!

    cut.findOwnProfilePictureMetadata(userId, user).apply {
      assertThat(body?.links?.getLink("small")?.get()?.href)
          .isEqualTo("http://localhost/default-profile-picture.png")
    }
  }

  @Test
  fun `deleting a profile picture creates tombstone messages for every version of the picture`() {
    eventStreamGenerator
        .submitUser("user")
        .submitProfilePicture("toBeDeleted")
        .submitProfilePicture("toBeDeleted", eventType = UPDATED) { it.smallAvailable = true }
        .submitProfilePicture("toBeDeleted", eventType = UPDATED) { it.fullAvailable = true }

    val userIdentifier = eventStreamGenerator.getIdentifier("user").asUserId()

    setAuthentication("user")
    val user = repositories.userRepository.findOneByIdentifier(userIdentifier)!!

    userEventStoreUtils.reset()

    cut.deleteProfilePicture(userIdentifier = userIdentifier, user = user)

    userEventStoreUtils.verifyContainsTombstoneMessageAndGet(3).also {
      validateTombstoneMessageKey(it[0], "toBeDeleted", 0)
      validateTombstoneMessageKey(it[1], "toBeDeleted", 1)
      validateTombstoneMessageKey(it[2], "toBeDeleted", 2)
    }
  }

  private fun validateTombstoneMessageKey(
      messageKey: MessageKeyAvro,
      reference: String,
      version: Long
  ) {
    getByReference(reference)
        .let {
          AggregateIdentifierAvro.newBuilder()
              .setType(it.type)
              .setVersion(version)
              .setIdentifier(it.identifier)
              .build()
        }
        .apply { assertThat(messageKey.aggregateIdentifier).isEqualTo(this) }
  }
}
