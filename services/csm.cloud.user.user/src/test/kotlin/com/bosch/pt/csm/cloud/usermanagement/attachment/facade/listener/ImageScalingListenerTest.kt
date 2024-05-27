/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.imagemanagement.image.profilePictureImageDeleted
import com.bosch.pt.csm.cloud.imagemanagement.image.profilePictureImageScaled
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.picture.asProfilePictureId
import com.bosch.pt.csm.cloud.usermanagement.user.picture.query.ProfilePictureQueryService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID.randomUUID
import org.apache.avro.Schema
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE

@EnableAllKafkaListeners
@SmartSiteSpringBootTest
class ImageScalingListenerTest : AbstractIntegrationTest() {

  @Autowired private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  @Autowired private lateinit var blobStorageProperties: BlobStorageProperties

  @Autowired private lateinit var imageScalingListener: ImageScalingListener

  @Autowired private lateinit var profilePictureQueryService: ProfilePictureQueryService

  @Value("classpath:img/exif.jpg") private lateinit var image: Resource

  @BeforeEach
  fun init() {
    every { blobStorageProperties.image } returns BlobStorageProperties.Image()
    every { blobStorageProperties.getImageFolder(any()) } answers { this.callOriginal() }

    eventStreamGenerator.submitSystemUserAndActivate().submitUserAndActivate("admin") {
      it.admin = true
    }
    useOnlineListener()

    setAuthentication("admin")
    userEventStoreUtils.reset()
  }

  @Test
  fun `image scaled event`() {
    submitUserAndProfilePicture()

    // Check state in db before updating it
    validateInitialDbState()

    // Mock
    every { azureBlobStorageRepository.read(any()) } returns image.inputStream

    // Send update event
    eventStreamGenerator.profilePictureImageScaled("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("picture").toString()
      it.path = "/images/users/${getIdentifier("user")}/picture"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that file size and available image sizes are updated
    val picture =
        profilePictureQueryService.findOneWithDetailsByIdentifier(
            getIdentifier("picture").asProfilePictureId())!!
    assertThat(picture.fileSize).isEqualTo(1000)
    assertThat(picture.isSmallAvailable()).isTrue()
    assertThat(picture.isFullAvailable()).isFalse()
    assertThat(picture.width).isEqualTo(4032)
    assertThat(picture.height).isEqualTo(3024)

    // Check kafka events
    userEventStoreUtils.verifyNumberOfEvents(1)
    userEventStoreUtils.verifyContains(
        UserPictureEventAvro::class.java, UserPictureEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `image scaled event unknown profile picture`() {
    submitUserAndProfilePicture()

    // Check state in db before updating it
    validateInitialDbState()

    // Send updated event with unknown identifier,
    // therefore the picture cannot be identified to be updated
    eventStreamGenerator.profilePictureImageScaled("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = randomUUID().toString()
      it.path = "/images/users/${getIdentifier("user")}/picture"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that profile picture is unchanged
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `image scaled event unknown user in path`() {
    submitUserAndProfilePicture()

    // Check state in db before updating it
    validateInitialDbState()

    // Send updated event with unknown identifier,
    // therefore the picture cannot be identified to be updated
    eventStreamGenerator.profilePictureImageScaled("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("picture").toString()
      it.path = "/images/users/${randomUUID()}/picture"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that profile picture is unchanged
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `image scaled event invalid path`() {
    submitUserAndProfilePicture()

    // Check state in db before updating it
    validateInitialDbState()

    // Send update event with invalid path, therefore the picture cannot be identified
    eventStreamGenerator.profilePictureImageScaled("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("picture").toString()
      it.path = "/invalid-path"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that profile picture is unchanged
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `image deleted event`() {
    submitUserAndProfilePicture()

    // Check that picture exists before deleting it
    validateInitialDbState()

    // Send deleted event
    val userIdentifier = getIdentifier("user")
    val pictureIdentifier = getIdentifier("picture")
    eventStreamGenerator.profilePictureImageDeleted("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = pictureIdentifier.toString()
      it.path = "/images/users/$userIdentifier/picture"
      it.identifier = pictureIdentifier.toString()
    }

    // Check that the profile picture is deleted
    assertThat(
            profilePictureQueryService.findOneWithDetailsByIdentifier(
                pictureIdentifier.asProfilePictureId()))
        .isNull()

    // Check kafka message
    userEventStoreUtils.verifyContainsTombstoneMessages(1, USERPICTURE.value, true)

    // Check blob storage
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "user/image/original/$userIdentifier/$pictureIdentifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "user/image/small/$userIdentifier/$pictureIdentifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "user/image/medium/$userIdentifier/$pictureIdentifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "user/image/fullhd/$userIdentifier/$pictureIdentifier")
    }
  }

  @Test
  fun `image deleted event unknown profile picture`() {
    submitUserAndProfilePicture()

    // Check that picture exists before deleting it
    validateInitialDbState()

    // Send deleted event with unknown identifier,
    // therefore the picture cannot be identified to be deleted
    eventStreamGenerator.profilePictureImageDeleted("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = randomUUID().toString()
      it.path = "/images/users/${getIdentifier("user")}/picture"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that the profile picture still exists
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `image deleted event unknown user in path`() {
    submitUserAndProfilePicture()

    // Check that picture exists before deleting it
    validateInitialDbState()

    // Send deleted event with unknown identifier,
    // therefore the picture cannot be identified to be deleted
    eventStreamGenerator.profilePictureImageDeleted("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("picture").toString()
      it.path = "/images/users/${randomUUID()}/picture"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that the profile picture still exists
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `image deleted event invalid path`() {
    submitUserAndProfilePicture()

    // Check that picture exists before deleting it
    validateInitialDbState()

    // Send deleted event with invalid path, therefore the picture cannot be identified
    eventStreamGenerator.profilePictureImageDeleted("image", "user") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("picture").toString()
      it.path = "/invalid-path"
      it.identifier = getIdentifier("picture").toString()
    }

    // Check that the profile picture still exists
    validateInitialDbState()
    userEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `ensure fail if transaction is running`() {
    val consumerRecord = mockk<ConsumerRecord<EventMessageKey, SpecificRecordBase?>>(relaxed = true)
    val ack = TestAcknowledgement()

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          transactionTemplate.executeWithoutResult {
            imageScalingListener.listenToImageEvents(consumerRecord, ack)
          }
        }
        .withMessage("No running transaction expected")
  }

  @Test
  fun `ensure fail if unexpected tombstone message`() {
    val consumerRecord = mockk<ConsumerRecord<EventMessageKey, SpecificRecordBase?>>(relaxed = true)
    every { consumerRecord.value() } returns null
    every { consumerRecord.key() } returns
        AggregateEventMessageKey(
            AggregateIdentifier(USERPICTURE.name, randomUUID(), 0L), randomUUID())

    val ack = TestAcknowledgement()

    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { imageScalingListener.listenToImageEvents(consumerRecord, ack) }
        .withMessage("Unexpected tombstone message found")
  }

  @Test
  fun `ensure fail if unknown avro message`() {
    val schema = mockk<Schema>(relaxed = true)
    every { schema.name } returns "Test Schema"
    val event = mockk<SpecificRecordBase>(relaxed = true)
    every { event.schema } returns schema
    val consumerRecord = mockk<ConsumerRecord<EventMessageKey, SpecificRecordBase?>>(relaxed = true)
    every { consumerRecord.value() } returns event
    every { consumerRecord.key() } returns
        AggregateEventMessageKey(
            AggregateIdentifier(USERPICTURE.name, randomUUID(), 0L), randomUUID())

    val ack = TestAcknowledgement()

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { imageScalingListener.listenToImageEvents(consumerRecord, ack) }
        .withMessage("Unknown avro message received: Test Schema")
  }

  private fun validateInitialDbState() {
    val pictureBeforeUpdate =
        profilePictureQueryService.findOneWithDetailsByIdentifier(
            getIdentifier("picture").asProfilePictureId())!!
    assertThat(pictureBeforeUpdate.fileSize).isEqualTo(-1)
    assertThat(pictureBeforeUpdate.isSmallAvailable()).isFalse()
    assertThat(pictureBeforeUpdate.isFullAvailable()).isFalse()
    assertThat(pictureBeforeUpdate.width).isEqualTo(150)
    assertThat(pictureBeforeUpdate.height).isEqualTo(100)
  }

  private fun submitUserAndProfilePicture() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture") {
      it.smallAvailable = false
      it.fullAvailable = false
      it.fileSize = -1
      it.width = 150
      it.height = 100
    }
  }
}
