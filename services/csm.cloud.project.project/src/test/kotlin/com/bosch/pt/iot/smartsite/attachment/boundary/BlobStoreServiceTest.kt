/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.boundary

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext.PROJECT
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.MEDIUM
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.iot.smartsite.attachment.util.MimeTypeDetector
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import java.net.URL
import java.util.Locale
import java.util.UUID.randomUUID
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.springframework.test.util.ReflectionTestUtils

@SmartSiteMockKTest
class BlobStoreServiceTest {

  @MockK(relaxed = true) private lateinit var blobRepository: AzureBlobStorageRepository

  @MockK(relaxed = true)
  private lateinit var quarantineBlobStorageRepository: QuarantineBlobStorageRepository

  @MockK(relaxed = true) private lateinit var mimeTypeDetector: MimeTypeDetector

  @MockK(relaxed = true) private lateinit var blobOwner: ImageBlobOwner

  @SpyK private var blobStorageProperties: BlobStorageProperties = BlobStorageProperties()

  private lateinit var cut: BlobStoreService

  @BeforeEach
  fun setup() {
    // Setup blob storage properties class values
    cut =
        BlobStoreService(
            blobStorageProperties,
            blobRepository,
            quarantineBlobStorageRepository,
            mimeTypeDetector,
            false)

    val folder =
        BlobStorageProperties.Folder().apply {
          original = "image/original"
          fullhd = "image/fullhd"
          medium = "image/medium"
          small = "image/small"
        }

    blobStorageProperties.image = BlobStorageProperties.Image().apply { folder }
  }

  @AfterEach
  fun cleanUp() {
    clearMocks(blobRepository, quarantineBlobStorageRepository, mimeTypeDetector, blobOwner)
  }

  private fun blobName(blobOwner: BlobOwner, resolution: ImageResolution): String {
    val pathSegments =
        arrayOf(
            blobOwner.getBoundedContext().name,
            imageFolder(resolution),
            blobOwner.getParentIdentifier().toString(),
            blobOwner.getIdentifierUuid().toString())

    return pathSegments.joinToString(separator = "/").lowercase(Locale.getDefault())
  }

  private fun imageFolder(resolution: ImageResolution): String =
      blobStorageProperties.getImageFolder(resolution)

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class)
  internal annotation class ImageResolutionsSource

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class, mode = EXCLUDE, names = ["ORIGINAL"])
  internal annotation class ImageResolutionsExceptOriginalSource

  @Nested
  @DisplayName("Save image")
  inner class SaveImage {

    @BeforeEach
    fun setup() {
      every { blobOwner.getParentIdentifier() } returns randomUUID()
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { mimeTypeDetector.detect(any()) } returns MIME_TYPE
      every { quarantineBlobStorageRepository.save(any(), any(), any(), any()) } answers
          {
            Blob(
                it.invocation.args[1] as String,
                it.invocation.args[0] as ByteArray,
                it.invocation.args[3] as BlobMetadata,
                it.invocation.args[2] as String)
          }
    }

    @Test
    fun verifySaveProfilePicture() {
      val project = mockk<Project>(relaxed = true)
      val projectId = randomUUID()
      every { project.identifier } returns projectId.asProjectId()

      val blobOwner = mockk<ProjectPicture>(relaxed = true)
      val profilePictureId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns projectId
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getIdentifierUuid() } returns profilePictureId
      every { blobOwner.project } returns project

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)

      assertThat(result).extracting { it.data }.isEqualTo(DATA)
      assertThat(result).extracting { it.metadata }.isEqualTo(BLOB_METADATA)
      assertThat(result)
          .extracting { it.blobName }
          .isEqualTo("images/projects/$projectId/picture/$profilePictureId")

      verify { quarantineBlobStorageRepository.save(DATA, any(), MIME_TYPE, any()) }
    }

    @Test
    fun verifySaveTaskAttachment() {
      val project = mockk<Project>(relaxed = true)
      val projectId = randomUUID()
      every { project.identifier } returns projectId.asProjectId()

      val task = mockk<Task>(relaxed = true)
      val taskId = randomUUID()
      every { task.identifier } returns taskId.asTaskId()
      every { task.project } returns project

      val blobOwner = mockk<TaskAttachment>(relaxed = true)
      val taskAttachmentId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns taskId
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getIdentifierUuid() } returns taskAttachmentId
      every { blobOwner.task } returns task

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)

      assertThat(result).extracting { it.data }.isEqualTo(DATA)
      assertThat(result).extracting { it.metadata }.isEqualTo(BLOB_METADATA)
      assertThat(result)
          .extracting { it.blobName }
          .isEqualTo("images/projects/$projectId/tasks/$taskId/$taskAttachmentId")

      verify { quarantineBlobStorageRepository.save(DATA, any(), MIME_TYPE, any()) }
    }

    @Test
    fun verifySaveTopicAttachment() {
      val project = mockk<Project>(relaxed = true)
      val projectId = randomUUID()
      every { project.identifier } returns projectId.asProjectId()

      val task = mockk<Task>(relaxed = true)
      val taskId = randomUUID()
      every { task.identifier } returns taskId.asTaskId()
      every { task.project } returns project

      val topic = mockk<Topic>(relaxed = true)
      val topicId = TopicId()
      every { topic.identifier } returns topicId
      every { topic.task } returns task

      val blobOwner = mockk<TopicAttachment>(relaxed = true)
      val topicAttachmentId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns taskId
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getIdentifierUuid() } returns topicAttachmentId
      every { blobOwner.task } returns task
      every { blobOwner.topic } returns topic

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)

      assertThat(result).extracting { it.data }.isEqualTo(DATA)
      assertThat(result).extracting { it.metadata }.isEqualTo(BLOB_METADATA)
      assertThat(result)
          .extracting { it.blobName }
          .isEqualTo("images/projects/$projectId/tasks/$taskId/topics/$topicId/$topicAttachmentId")

      verify { quarantineBlobStorageRepository.save(DATA, any(), MIME_TYPE, any()) }
    }

    @Test
    fun verifySaveMessageAttachment() {
      val project = mockk<Project>(relaxed = true)
      val projectId = randomUUID()
      every { project.identifier } returns projectId.asProjectId()

      val task = mockk<Task>(relaxed = true)
      val taskId = randomUUID()
      every { task.identifier } returns taskId.asTaskId()
      every { task.project } returns project

      val topic = mockk<Topic>(relaxed = true)
      val topicId = TopicId()
      every { topic.identifier } returns topicId
      every { topic.task } returns task

      val message = mockk<Message>(relaxed = true)
      val messageId = MessageId()
      every { message.identifier } returns messageId
      every { message.topic } returns topic

      val blobOwner = mockk<MessageAttachment>(relaxed = true)
      val messageAttachmentId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns taskId
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getIdentifierUuid() } returns messageAttachmentId
      every { blobOwner.task } returns task
      every { blobOwner.topic } returns topic
      every { blobOwner.message } returns message

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)

      assertThat(result).extracting { it.data }.isEqualTo(DATA)
      assertThat(result).extracting { it.metadata }.isEqualTo(BLOB_METADATA)
      assertThat(result)
          .extracting { it.blobName }
          .isEqualTo(
              "images/projects/$projectId" +
                  "/tasks/$taskId" +
                  "/topics/$topicId" +
                  "/messages/$messageId" +
                  "/$messageAttachmentId")

      verify { quarantineBlobStorageRepository.save(DATA, any(), MIME_TYPE, any()) }
    }

    @Test
    fun detectsAndSavesMimeType() {
      val project = mockk<Project>(relaxed = true)
      val projectId = randomUUID()
      every { project.identifier } returns projectId.asProjectId()

      val blobOwner = mockk<ProjectPicture>(relaxed = true)
      val profilePictureId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns projectId
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getIdentifierUuid() } returns profilePictureId
      every { blobOwner.project } returns project

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)
      assertThat(result).extracting { it.mimeType }.isEqualTo(MIME_TYPE)
    }
  }

  @Nested
  @DisplayName("generate blob url")
  inner class GenerateBlobAccessUrl {

    private lateinit var accessUrl: URL

    @BeforeEach
    fun setup() {
      every { blobOwner.getIdentifierUuid() } returns randomUUID()
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getParentIdentifier() } returns randomUUID()

      accessUrl = URL("https://blob-store")
    }

    @ParameterizedTest
    @ImageResolutionsExceptOriginalSource
    fun delegatesToBlobRepositoryWithExpectedBlobName(resolution: ImageResolution) {
      every { blobRepository.generateSignedUrl(any()) } returns accessUrl

      val url = cut.generateSignedUrlForImage(blobOwner, resolution)
      assertThat(url).isNotNull

      val expectedBlobName = blobName(blobOwner, resolution)
      verify { blobRepository.generateSignedUrl(expectedBlobName) }
    }
  }

  @Nested
  @DisplayName("delete image")
  inner class DeleteBlob {
    @BeforeEach
    fun setup() {
      every { blobOwner.getIdentifierUuid() } returns randomUUID()
      every { blobOwner.getBoundedContext() } returns PROJECT
      every { blobOwner.getParentIdentifier() } returns randomUUID()
    }

    @DisplayName("with resolution")
    @ParameterizedTest
    @ImageResolutionsSource
    fun deleteImage(resolution: ImageResolution) {
      cut.deleteImageIfExists(blobOwner, resolution)

      verify { blobRepository.deleteIfExists(any()) }
    }
  }

  @Nested
  @DisplayName("delete blobs in directory")
  inner class DeleteBlobsInDirectory {

    @DisplayName("with expected directory name")
    @Test
    fun callsBlobRepositoryWithExpectedDirectoryName() {
      val dir = randomUUID().toString()

      cut.deleteImagesInDirectory(dir)

      var directoryName = cut.getDirectoryName(dir, SMALL)
      verify { blobRepository.deleteBlobsInDirectory(directoryName) }
      directoryName = cut.getDirectoryName(dir, MEDIUM)
      verify { blobRepository.deleteBlobsInDirectory(directoryName) }
      directoryName = cut.getDirectoryName(dir, FULL)
      verify { blobRepository.deleteBlobsInDirectory(directoryName) }
      directoryName = cut.getDirectoryName(dir, ORIGINAL)
      verify { blobRepository.deleteBlobsInDirectory(directoryName) }
    }
  }

  // Added as separate nested class because this code is not compatible with the existing mocking of
  // the other nested classes
  @Nested
  @DisplayName("blocks modifying operations")
  inner class BlockModifyingOperations {

    @BeforeEach
    fun setup() {
      ReflectionTestUtils.setField(cut, "blockModifyingOperations", true)
    }

    @Test
    fun verifyBlockSaveImage() {
      assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
        cut.saveImage(DATA, blobOwner, BLOB_METADATA)
      }
    }

    @Test
    fun verifyBlockDeleteImageIfExists() {
      assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
        cut.deleteImageIfExists(blobOwner, ORIGINAL)
      }
    }

    @Test
    fun verifyBlockDeleteImagesInDirectory() {
      val dir = randomUUID().toString()

      assertThatExceptionOfType(BlockOperationsException::class.java).isThrownBy {
        cut.deleteImagesInDirectory(dir)
      }
    }
  }

  companion object {
    private val BLOB_METADATA = BlobMetadata.fromMap(emptyMap())
    private val DATA = "myData".toByteArray()

    private const val MIME_TYPE = "image/jpeg"
  }
}
