/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.PROJECT_PICTURE
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.imagemanagement.image.messageAttachmentImageDeleted
import com.bosch.pt.csm.cloud.imagemanagement.image.messageAttachmentImageScaled
import com.bosch.pt.csm.cloud.imagemanagement.image.projectPictureImageDeleted
import com.bosch.pt.csm.cloud.imagemanagement.image.projectPictureImageScaled
import com.bosch.pt.csm.cloud.imagemanagement.image.taskAttachmentImageDeleted
import com.bosch.pt.csm.cloud.imagemanagement.image.topicAttachmentImageDeleted
import com.bosch.pt.csm.cloud.imagemanagement.image.topicAttachmentImageScaled
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import com.azure.storage.blob.specialized.BlobInputStream
import com.azure.storage.blob.specialized.BlockBlobClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID
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
class ImageScalingListenerTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var blobStorageProperties: BlobStorageProperties

  @Autowired private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  @Autowired private lateinit var projectPictureService: ProjectPictureService

  @Autowired private lateinit var messageAttachmentService: AttachmentService

  @Autowired private lateinit var imageScalingListener: ImageScalingListener

  @Value("classpath:img/exif.jpg") private lateinit var image: Resource

  @BeforeEach
  fun init() {
    every { blobStorageProperties.image } returns BlobStorageProperties.Image()
    every { blobStorageProperties.getImageFolder(any()) } answers { this.callOriginal() }

    eventStreamGenerator
        .submitSystemUserAndActivate()
        .submitUserAndActivate("user")
        .submitProjectWithPictureAndAttachments()
        .run {
          setAuthentication("user")
          // Check state in db before updating it
          validateInitialDbState()
          setAuthentication("system")
        }

    projectEventStoreUtils.reset()
  }

  @Test
  fun `project picture image scaled event`() {
    mockBlobStorage(false)

    // Send update event
    eventStreamGenerator.projectPictureImageScaled("projectImage", "project") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("projectPicture").toString()
      it.path = "/images/projects/${getIdentifier("project")}/picture"
      it.identifier = getIdentifier("projectPicture").toString()
    }

    // Check that file size and available image sizes are updated
    val picture = projectPictureService.findProjectPicture(getIdentifier("project").asProjectId())
    assertThat(picture.fileSize).isEqualTo(1000)
    assertThat(picture.isSmallAvailable()).isTrue()
    assertThat(picture.isFullAvailable()).isTrue()
    assertThat(picture.width).isEqualTo(4032)
    assertThat(picture.height).isEqualTo(3024)

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        ProjectPictureEventAvro::class.java, ProjectPictureEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `project picture image deleted event`() {
    // Ensure state before deletion
    val projectIdentifier = getIdentifier("project")
    val projectPictureIdentifier = getIdentifier("projectPicture")
    assertThat(projectPictureService.findProjectPicture(projectIdentifier.asProjectId())).isNotNull

    // Send delete event
    eventStreamGenerator.projectPictureImageDeleted("projectImage", "project") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = projectPictureIdentifier.toString()
      it.path = "/images/projects/$projectIdentifier/picture"
      it.identifier = projectPictureIdentifier.toString()
    }

    // Ensure state after deletion
    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { projectPictureService.findProjectPicture(projectIdentifier.asProjectId()) }
        .withMessageKey(PROJECT_VALIDATION_ERROR_PROJECT_PICTURE_NOT_FOUND)

    verifyAttachmentIsDeletedFromBlobStorage(projectIdentifier, projectPictureIdentifier)

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        ProjectPictureEventAvro::class.java, ProjectPictureEventEnumAvro.DELETED, 1)
  }

  @Test
  fun `message attachment image scaled event`() {
    mockBlobStorage()

    // Send update event
    eventStreamGenerator.messageAttachmentImageScaled("messageImage", "project") {
      it.contentLength = 1234
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("messageAttachment").toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path =
          "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
              "/topics/${getIdentifier("topic")}/messages/${getIdentifier("message")}"
      it.identifier = getIdentifier("messageAttachment").toString()
    }

    // Check that file size and available image sizes are updated
    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("messageAttachment")))
        .let {
          assertThat(it.fileSize).isEqualTo(1234)
          assertThat(it.isSmallAvailable()).isTrue()
          assertThat(it.isFullAvailable()).isTrue()
          assertThat(it.imageWidth).isEqualTo(4032)
          assertThat(it.imageHeight).isEqualTo(3024)
          assertThat(it.captureDate?.toInstant())
              .isEqualTo(LocalDateTime.of(2016, 11, 28, 19, 44, 48, 0).toInstant(ZoneOffset.UTC))
        }

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        MessageAttachmentEventAvro::class.java, MessageAttachmentEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `message attachment image deleted event`() {
    // Ensure state before deletion
    val attachmentIdentifier = getIdentifier("messageAttachment")
    val attachment = messageAttachmentService.findAttachment(attachmentIdentifier)
    assertThat(attachment).isNotNull

    // Send delete event
    eventStreamGenerator.messageAttachmentImageDeleted("messageImage", "project") {
      it.contentLength = 1234
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = attachmentIdentifier.toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path =
          "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
              "/topics/${getIdentifier("topic")}/messages/${getIdentifier("message")}"
      it.identifier = attachmentIdentifier.toString()
    }

    // Ensure state after deletion
    assertThat(messageAttachmentService.findAttachment(attachmentIdentifier)).isNull()
    assertThat(
            repositories.messageRepository.findOneByIdentifier(attachment!!.message!!.identifier))
        .isNull()

    verifyAttachmentIsDeletedFromBlobStorage(getIdentifier("task"), attachmentIdentifier)

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        MessageEventAvro::class.java, MessageEventEnumAvro.DELETED, 1)
  }

  @Test
  fun `topic attachment image scaled event`() {
    mockBlobStorage()

    // Send update event
    eventStreamGenerator.topicAttachmentImageScaled("topicImage", "project") {
      it.contentLength = 2345
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("topicAttachment").toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path =
          "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
              "/topics/${getIdentifier("topic")}"
      it.identifier = getIdentifier("topicAttachment").toString()
    }

    // Check that file size and available image sizes are updated
    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("topicAttachment"))).let {
      assertThat(it.fileSize).isEqualTo(2345)
      assertThat(it.isSmallAvailable()).isTrue()
      assertThat(it.isFullAvailable()).isTrue()
      assertThat(it.imageWidth).isEqualTo(4032)
      assertThat(it.imageHeight).isEqualTo(3024)
      assertThat(it.captureDate?.toInstant())
          .isEqualTo(LocalDateTime.of(2016, 11, 28, 19, 44, 48, 0).toInstant(ZoneOffset.UTC))
    }

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        TopicAttachmentEventAvro::class.java, TopicAttachmentEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `topic attachment image deleted event`() {
    // Check state before deletion
    val attachmentIdentifier = getIdentifier("topicAttachment")
    assertThat(
            repositories.topicAttachmentRepository.findOneWithDetailsByIdentifier(
                attachmentIdentifier))
        .isNotNull

    // Send delete event
    eventStreamGenerator.topicAttachmentImageDeleted("topicImage", "project") {
      it.contentLength = 2345
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = attachmentIdentifier.toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path =
          "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
              "/topics/${getIdentifier("topic")}"
      it.identifier = attachmentIdentifier.toString()
    }

    // Ensure state after deletion
    assertThat(
            repositories.topicAttachmentRepository.findOneWithDetailsByIdentifier(
                attachmentIdentifier))
        .isNull()
    assertThat(repositories.topicRepository.findOneByIdentifier(getIdentifier("topic").asTopicId()))
        .isNull()

    verifyAttachmentIsDeletedFromBlobStorage(getIdentifier("task"), attachmentIdentifier)

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        TopicEventG2Avro::class.java, TopicEventEnumAvro.DELETED, 1)
  }

  @Test
  fun `task attachment image scaled event`() {
    mockBlobStorage()

    // Send update event
    eventStreamGenerator.topicAttachmentImageScaled("taskImage", "project") {
      it.contentLength = 3456
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("taskAttachment").toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path = "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}"
      it.identifier = getIdentifier("taskAttachment").toString()
    }

    // Check that file size and available image sizes are updated
    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("taskAttachment"))).let {
      assertThat(it.fileSize).isEqualTo(3456)
      assertThat(it.isSmallAvailable()).isTrue()
      assertThat(it.isFullAvailable()).isTrue()
      assertThat(it.imageWidth).isEqualTo(4032)
      assertThat(it.imageHeight).isEqualTo(3024)
      assertThat(it.captureDate?.toInstant())
          .isEqualTo(LocalDateTime.of(2016, 11, 28, 19, 44, 48, 0).toInstant(ZoneOffset.UTC))
    }

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        TaskAttachmentEventAvro::class.java, TaskAttachmentEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `task attachment image deleted event`() {
    // Ensure state before deletion
    val attachmentIdentifier = getIdentifier("taskAttachment")
    val attachment =
        repositories.taskAttachmentRepository.findOneWithDetailsByIdentifier(attachmentIdentifier)
    assertThat(attachment).isNotNull

    // Send delete event
    eventStreamGenerator.topicAttachmentImageDeleted("taskImage", "project") {
      it.contentLength = 3456
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = attachmentIdentifier.toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path = "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}"
      it.identifier = attachmentIdentifier.toString()
    }

    // Ensure state after deletion
    assertThat(
            repositories.taskAttachmentRepository.findOneWithDetailsByIdentifier(
                attachmentIdentifier))
        .isNull()

    verifyAttachmentIsDeletedFromBlobStorage(getIdentifier("task"), attachmentIdentifier)

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        TaskAttachmentEventAvro::class.java, TaskAttachmentEventEnumAvro.DELETED, 1)
  }

  @Test
  fun `task attachment without timezone info image scaled event`() {
    mockBlobStorage(false)

    // Send update event
    eventStreamGenerator.topicAttachmentImageScaled("taskImage", "project") {
      it.contentLength = 3456
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("taskAttachment").toString()
      // /images/projects/([^/]*)/tasks/([^/]*)/topics/([^/]*)/messages/([^/]*)
      it.path = "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}"
      it.identifier = getIdentifier("taskAttachment").toString()
    }

    // Check that file size and available image sizes are updated
    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("taskAttachment"))).let {
      assertThat(it.fileSize).isEqualTo(3456)
      assertThat(it.isSmallAvailable()).isTrue()
      assertThat(it.isFullAvailable()).isTrue()
      assertThat(it.imageWidth).isEqualTo(4032)
      assertThat(it.imageHeight).isEqualTo(3024)
      assertThat(it.captureDate?.toInstant())
          .isEqualTo(LocalDateTime.of(2016, 11, 28, 14, 44, 48, 0).toInstant(ZoneOffset.UTC))
    }

    // Check kafka messages
    projectEventStoreUtils.verifyNumberOfEvents(1)
    projectEventStoreUtils.verifyContains(
        TaskAttachmentEventAvro::class.java, TaskAttachmentEventEnumAvro.UPDATED, 1)
  }

  @Test
  fun `wrong project id`() {

    eventStreamGenerator
        .projectPictureImageScaled("projectImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("projectPicture").toString()
          it.path = "/images/projects/${randomUUID()}/picture"
          it.identifier = getIdentifier("projectPicture").toString()
        }
        .messageAttachmentImageDeleted("messageImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("messageAttachment").toString()
          it.path =
              "/images/projects/${randomUUID()}/tasks/${getIdentifier("task")}" +
                  "/topics/${getIdentifier("topic")}/messages/${getIdentifier("message")}"
          it.identifier = getIdentifier("messageAttachment").toString()
        }
        .topicAttachmentImageDeleted("topicImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("topicAttachment").toString()
          it.path =
              "/images/projects/${randomUUID()}/tasks/${getIdentifier("task")}" +
                  "/topics/${getIdentifier("topic")}"
          it.identifier = getIdentifier("topicAttachment").toString()
        }
        .taskAttachmentImageDeleted("taskImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("taskAttachment").toString()
          it.path = "/images/projects/${randomUUID()}/tasks/${getIdentifier("task")}"
          it.identifier = getIdentifier("taskAttachment").toString()
        }

    validateInitialDbState()
  }

  @Test
  fun `wrong task id`() {

    eventStreamGenerator
        .messageAttachmentImageDeleted("messageImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("messageAttachment").toString()
          it.path =
              "/images/projects/${getIdentifier("project")}/tasks/${randomUUID()}" +
                  "/topics/${getIdentifier("topic")}/messages/${getIdentifier("message")}"
          it.identifier = getIdentifier("messageAttachment").toString()
        }
        .topicAttachmentImageDeleted("topicImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("topicAttachment").toString()
          it.path =
              "/images/projects/${getIdentifier("project")}/tasks/${randomUUID()}" +
                  "/topics/${getIdentifier("topic")}"
          it.identifier = getIdentifier("topicAttachment").toString()
        }
        .taskAttachmentImageDeleted("taskImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("taskAttachment").toString()
          it.path = "/images/projects/${getIdentifier("project")}/tasks/${randomUUID()}"
          it.identifier = getIdentifier("taskAttachment").toString()
        }

    validateInitialDbState()
  }

  @Test
  fun `wrong topic id`() {

    eventStreamGenerator
        .messageAttachmentImageDeleted("messageImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("messageAttachment").toString()
          it.path =
              "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
                  "/topics/${randomUUID()}/messages/${getIdentifier("message")}"
          it.identifier = getIdentifier("messageAttachment").toString()
        }
        .topicAttachmentImageDeleted("topicImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("topicAttachment").toString()
          it.path =
              "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
                  "/topics/${randomUUID()}"
          it.identifier = getIdentifier("topicAttachment").toString()
        }

    validateInitialDbState()
  }

  @Test
  fun `wrong message id`() {

    eventStreamGenerator.messageAttachmentImageDeleted("messageImage", "project") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = getIdentifier("messageAttachment").toString()
      it.path =
          "/images/projects/${getIdentifier("project")}/tasks/${getIdentifier("task")}" +
              "/topics/${getIdentifier("topic")}/messages/${randomUUID()}"
      it.identifier = getIdentifier("messageAttachment").toString()
    }

    validateInitialDbState()
  }

  @Test
  fun `image deleted event unknown reference in path`() {

    // Send deleted event with unknown identifier,
    // therefore the picture cannot be identified to be deleted
    eventStreamGenerator.projectPictureImageDeleted("taskImage", "project") {
      it.contentLength = 1000
      it.contentType = IMAGE_JPEG_VALUE
      it.filename = randomUUID().toString()
      it.path = "/images/projects/${getIdentifier("project")}/tasks/${randomUUID()}"
      it.identifier = getIdentifier("taskAttachment").toString()
    }

    // Check that the profile picture still exists
    validateInitialDbState()
  }

  @Test
  fun `image deleted event invalid path`() {

    // Send deleted event with invalid path, therefore the picture cannot be identified
    eventStreamGenerator
        .projectPictureImageDeleted("projectImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("projectPicture").toString()
          it.path = "/invalid-path"
          it.identifier = getIdentifier("projectPicture").toString()
        }
        .messageAttachmentImageDeleted("messageImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("messageAttachment").toString()
          it.path = "/invalid-path"
          it.identifier = getIdentifier("messageAttachment").toString()
        }
        .topicAttachmentImageDeleted("topicImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("topicAttachment").toString()
          it.path = "/invalid-path"
          it.identifier = getIdentifier("topicAttachment").toString()
        }
        .taskAttachmentImageDeleted("taskImage", "project") {
          it.contentLength = 1000
          it.contentType = IMAGE_JPEG_VALUE
          it.filename = getIdentifier("taskAttachment").toString()
          it.path = "/invalid-path"
          it.identifier = getIdentifier("taskAttachment").toString()
        }

    // Check that the profile picture still exists
    validateInitialDbState()
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
  fun `ensure fail if unknown avro message`() {
    val schema = mockk<Schema>(relaxed = true)
    every { schema.name } returns "Test Schema"
    val event = mockk<SpecificRecordBase>(relaxed = true)
    every { event.schema } returns schema
    val consumerRecord = mockk<ConsumerRecord<EventMessageKey, SpecificRecordBase?>>(relaxed = true)
    every { consumerRecord.value() } returns event
    every { consumerRecord.key() } returns
        AggregateEventMessageKey(
            AggregateIdentifier(PROJECT_PICTURE.name, randomUUID(), 0L), randomUUID())

    val ack = TestAcknowledgement()

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { imageScalingListener.listenToImageEvents(consumerRecord, ack) }
        .withMessage("Unknown Avro message received: Test Schema")
  }

  private fun validateInitialDbState() {
    requireNotNull(projectPictureService.findProjectPicture(getIdentifier("project").asProjectId()))
        .let {
          assertThat(it.fileSize).isEqualTo(-1)
          assertThat(it.isSmallAvailable()).isFalse()
          assertThat(it.isFullAvailable()).isFalse()
          assertThat(it.width).isEqualTo(150)
          assertThat(it.height).isEqualTo(100)
        }

    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("messageAttachment")))
        .let {
          assertThat(it.fileSize).isEqualTo(-1)
          assertThat(it.isSmallAvailable()).isFalse()
          assertThat(it.isFullAvailable()).isFalse()
          assertThat(it.imageWidth).isEqualTo(1920)
          assertThat(it.imageHeight).isEqualTo(1080)
        }

    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("topicAttachment"))).let {
      assertThat(it.fileSize).isEqualTo(-1)
      assertThat(it.isSmallAvailable()).isFalse()
      assertThat(it.isFullAvailable()).isFalse()
      assertThat(it.imageWidth).isEqualTo(800)
      assertThat(it.imageHeight).isEqualTo(600)
    }

    requireNotNull(messageAttachmentService.findAttachment(getIdentifier("taskAttachment"))).let {
      assertThat(it.fileSize).isEqualTo(-1)
      assertThat(it.isSmallAvailable()).isFalse()
      assertThat(it.isFullAvailable()).isFalse()
      assertThat(it.imageWidth).isEqualTo(640)
      assertThat(it.imageHeight).isEqualTo(480)
    }
  }

  private fun EventStreamGenerator.submitProjectWithPictureAndAttachments() =
      this.setupDatasetTestData()
          .submitProjectPicture {
            it.smallAvailable = false
            it.fullAvailable = false
            it.fileSize = -1
            it.width = 150
            it.height = 100
          }
          .submitMessageAttachment {
            it.attachmentBuilder =
                AttachmentAvro.newBuilder()
                    .setCaptureDate(Instant.now().toEpochMilli())
                    .setFileName("messagePicture.jpg")
                    .setFileSize(-1)
                    .setFullAvailable(false)
                    .setSmallAvailable(false)
                    .setWidth(1920)
                    .setHeight(1080)
          }
          .submitTopicAttachment {
            it.attachmentBuilder =
                AttachmentAvro.newBuilder()
                    .setCaptureDate(Instant.now().toEpochMilli())
                    .setFileName("topicPicture.jpg")
                    .setFileSize(-1)
                    .setFullAvailable(false)
                    .setSmallAvailable(false)
                    .setWidth(800)
                    .setHeight(600)
          }
          .submitTaskAttachment {
            it.attachmentBuilder =
                AttachmentAvro.newBuilder()
                    .setCaptureDate(Instant.now().toEpochMilli())
                    .setFileName("taskPicture.jpg")
                    .setFileSize(-1)
                    .setFullAvailable(false)
                    .setSmallAvailable(false)
                    .setWidth(640)
                    .setHeight(480)
          }
          .submitParticipantG3()

  private fun mockBlobStorage(includeTimezone: Boolean = true) {
    val blockBlobClient =
        mockk<BlockBlobClient>().apply {
          every { exists() } returns true
          if (includeTimezone) {
            every { tags } returns
                mutableMapOf("timezone" to TimeZone.getTimeZone("America/New_York").id)
          } else {
            every { tags } returns mutableMapOf()
          }

          val blobInputStream = mockk<BlobInputStream>(relaxed = true)
          every { blobInputStream.readAllBytes() } returns image.inputStream.readAllBytes()
          every { openInputStream() } returns blobInputStream
        }
    every { azureBlobStorageRepository.getBlockBlobClient(any()) } returns blockBlobClient
  }

  // Ensure that all resolutions (as defined in enum) are deleted
  private fun verifyAttachmentIsDeletedFromBlobStorage(parentIdentifier: UUID, identifier: UUID) {
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "project/image/original/$parentIdentifier/$identifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists("project/image/small/$parentIdentifier/$identifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "project/image/medium/$parentIdentifier/$identifier")
    }
    verify(exactly = 1) {
      azureBlobStorageRepository.deleteIfExists(
          "project/image/fullhd/$parentIdentifier/$identifier")
    }
  }
}
