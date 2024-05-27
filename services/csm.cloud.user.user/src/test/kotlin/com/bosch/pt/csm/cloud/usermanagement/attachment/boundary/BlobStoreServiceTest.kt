/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext.USER
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.MimeTypeDetector
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.net.URL
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
import org.springframework.test.util.ReflectionTestUtils

/** Test to verify expected behavior of the blob store service. */
@SmartSiteMockKTest
internal class BlobStoreServiceTest {

  @MockK(relaxed = true) private lateinit var blobRepository: AzureBlobStorageRepository

  @MockK(relaxed = true)
  private lateinit var quarantineBlobStorageRepository: QuarantineBlobStorageRepository

  @MockK(relaxed = true) private lateinit var mimeTypeDetector: MimeTypeDetector

  @MockK(relaxed = true) private lateinit var blobOwner: ImageBlobOwner

  private var blobStorageProperties: BlobStorageProperties = spyk(BlobStorageProperties())

  private lateinit var cut: BlobStoreService

  @BeforeEach
  fun setup() {
    cut =
        BlobStoreService(
            blobStorageProperties,
            blobRepository,
            quarantineBlobStorageRepository,
            mimeTypeDetector)

    // Setup blob storage properties class values
    val folder = BlobStorageProperties.Folder()
    ReflectionTestUtils.setField(folder, "original", "image/original")
    ReflectionTestUtils.setField(folder, "fullhd", "image/fullhd")
    ReflectionTestUtils.setField(folder, "medium", "image/medium")
    ReflectionTestUtils.setField(folder, "small", "image/small")
    val image = BlobStorageProperties.Image()
    ReflectionTestUtils.setField(image, "folder", folder)
    ReflectionTestUtils.setField(blobStorageProperties, "image", image)
  }

  @AfterEach
  fun cleanup() {
    clearMocks(blobRepository, quarantineBlobStorageRepository, mimeTypeDetector, blobOwner)
  }

  private fun blobName(blobOwner: BlobOwner, resolution: ImageResolution): String {
    val pathSegments =
        arrayOf(
            blobOwner.getBoundedContext().name,
            imageFolder(resolution),
            blobOwner.getParentIdentifier().toString(),
            blobOwner.getIdentifierUuid().toString())
    val path = java.lang.String.join("/", *pathSegments)
    return path.lowercase()
  }

  private fun imageFolder(resolution: ImageResolution): String =
      blobStorageProperties.getImageFolder(resolution)

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class)
  internal annotation class ImageResolutionsSource

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = ImageResolution::class, mode = EnumSource.Mode.EXCLUDE, names = ["ORIGINAL"])
  internal annotation class ImageResolutionsExceptOriginalSource

  @Nested
  @DisplayName("Save image")
  inner class SaveImage {

    @BeforeEach
    fun setup() {

      every { blobOwner.getBoundedContext() } returns USER
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
    fun verifySaveImage() {
      val userId = randomUUID()

      val blobOwner = mockk<ProfilePicture>(relaxed = true)
      val profilePictureId = randomUUID()
      every { blobOwner.getParentIdentifier() } returns userId
      every { blobOwner.getBoundedContext() } returns USER
      every { blobOwner.getIdentifierUuid() } returns profilePictureId

      val result = cut.saveImage(DATA, blobOwner, BLOB_METADATA)
      assertThat(result).extracting { it.data }.isEqualTo(DATA)
      assertThat(result).extracting { it.mimeType }.isEqualTo(MIME_TYPE)
      assertThat(result).extracting { it.metadata }.isEqualTo(BLOB_METADATA)
      assertThat(result)
          .extracting { it.blobName }
          .isEqualTo("images/users/$userId/picture/$profilePictureId")

      verify { quarantineBlobStorageRepository.save(DATA, any(), MIME_TYPE, any()) }
    }
  }

  @Nested
  @DisplayName("generate blob url")
  inner class GenerateBlobAccessUrl {

    private lateinit var accessUrl: URL

    @BeforeEach
    @Throws(Exception::class)
    fun setup() {
      every { blobOwner.getIdentifierUuid() } returns randomUUID()
      every { blobOwner.getBoundedContext() } returns USER
      every { blobOwner.getParentIdentifier() } returns randomUUID()
      accessUrl = URL("https://blob-store")
    }

    @ParameterizedTest
    @ImageResolutionsExceptOriginalSource
    fun delegatesToBlobRepositoryWithExpectedBlobName(resolution: ImageResolution) {
      every { blobRepository.generateSignedUrl(any()) } returns accessUrl
      val url = cut.generateSignedUrlForImage(blobOwner, resolution)
      val blobName = blobName(blobOwner, resolution)
      verify { blobRepository.generateSignedUrl(blobName) }
      assertThat(url).isNotNull
    }
  }

  @Nested
  @DisplayName("delete image")
  inner class DeleteBlob {

    @BeforeEach
    fun setup() {
      every { blobOwner.getIdentifierUuid() } returns randomUUID()
      every { blobOwner.getBoundedContext() } returns USER
      every { blobOwner.getParentIdentifier() } returns randomUUID()
    }

    @ParameterizedTest
    @ImageResolutionsSource
    fun `with resolution`(resolution: ImageResolution) {
      val deleteBlobSlot = slot<String>()
      every { blobRepository.deleteIfExists(capture(deleteBlobSlot)) } returns false

      cut.deleteImageIfExists(blobOwner, resolution)
      assertThat(deleteBlobSlot.captured).containsPattern(".*" + imageFolder(resolution) + ".*")
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
        cut.deleteImageIfExists(blobOwner, ImageResolution.ORIGINAL)
      }
    }
  }

  companion object {
    private const val MIME_TYPE = "image/jpeg"
    private val DATA = "myData".toByteArray()
    private val BLOB_METADATA = BlobMetadata.fromMap(emptyMap())
  }
}
