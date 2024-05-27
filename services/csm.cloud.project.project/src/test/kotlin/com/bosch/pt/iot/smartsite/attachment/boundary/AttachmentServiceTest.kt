/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.attachment.boundary

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.attachment.model.BlobBuilder.Companion.blob
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.ORIGINAL
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.taskattachment.model.AttachmentBuilder.Companion.attachment
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE

@SmartSiteMockKTest
internal class AttachmentServiceTest {

  @MockK(relaxed = true) private lateinit var blobStoreService: BlobStoreService

  @MockK(relaxed = true) private lateinit var attachmentRepository: AttachmentRepository

  @InjectMockKs private lateinit var cut: AttachmentService

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = AttachmentImageResolution::class, mode = EXCLUDE, names = ["ORIGINAL"])
  internal annotation class SupportedImageResolutionsSource

  @Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
  @Retention(RUNTIME)
  @EnumSource(value = AttachmentImageResolution::class)
  internal annotation class SupportedImageResolutionsWithOriginalSource

  @Nested
  @DisplayName("save attachment")
  inner class SaveAttachment {

    lateinit var savedBlob: Blob
    val attachment = attachment().build()

    private val blobData = "myData".toByteArray()
    private val blobMetadata = BlobMetadata.fromMap(emptyMap())

    @BeforeEach
    fun setup() {
      every { blobStoreService.saveImage(any(), any(), any()) } answers
          {
            blob()
                .withData(it.invocation.args[0] as ByteArray)
                .withMetadata(it.invocation.args[2] as BlobMetadata)
                .build()
          }

      savedBlob = cut.storeBlob(blobData, attachment, blobMetadata)
    }

    @Test
    fun returnsSavedEntity() {
      assertThat(savedBlob.data).isEqualTo(blobData)
      assertThat(savedBlob.metadata).isEqualTo(blobMetadata)
    }

    @Test
    fun storesBlob() {
      verify { blobStoreService.saveImage(blobData, attachment, blobMetadata) }
    }

    @Nested
    @DisplayName("generate blob url")
    inner class GenerateBlobAccessUrl {

      private val attachmentBuilder = attachment()
      private lateinit var accessUrl: URL

      @BeforeEach
      fun setup() {
        accessUrl = URL("https://blob-store")
      }

      @Test
      fun throwsExceptionWhenAttachmentDoesNotExist() {
        every { attachmentRepository.findAttachmentByIdentifier(any()) } returns null

        assertThrows(
            AggregateNotFoundException::class.java,
            { cut.generateBlobAccessUrl(randomUUID(), ORIGINAL) },
            ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)
      }

      @ParameterizedTest
      @SupportedImageResolutionsWithOriginalSource
      fun throwsExceptionWhenBlobDoesNotExist(resolution: AttachmentImageResolution) {
        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns null
        every { attachmentRepository.findAttachmentByIdentifier(any()) } returns
            attachmentBuilder.build()

        assertThrows(
            AggregateNotFoundException::class.java,
            { cut.generateBlobAccessUrl(randomUUID(), resolution) },
            ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)
      }

      @ParameterizedTest
      @SupportedImageResolutionsSource
      fun returnsOriginalWhenResolutionIsUnavailable(resolution: AttachmentImageResolution) {
        val attachment = attachmentBuilder.withResolutionsAvailable(resolution).build()

        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
        every { attachmentRepository.findAttachmentByIdentifier(any()) } returns attachment

        val url = cut.generateBlobAccessUrl(randomUUID(), ORIGINAL)
        assertThat(url).isEqualTo(accessUrl)

        verify { blobStoreService.generateSignedUrlForImage(any(), ImageResolution.ORIGINAL) }
      }

      @ParameterizedTest
      @SupportedImageResolutionsWithOriginalSource
      fun returnsGivenResolutionWhenAvailable(resolution: AttachmentImageResolution) {
        val attachment = attachmentBuilder.withResolutionsAvailable(resolution).build()

        every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns accessUrl
        every { attachmentRepository.findAttachmentByIdentifier(any()) } returns attachment

        val url = cut.generateBlobAccessUrl(randomUUID(), resolution)
        assertThat(url).isEqualTo(accessUrl)

        verify { blobStoreService.generateSignedUrlForImage(any(), resolution.imageResolution) }
      }
    }

    @Nested
    @DisplayName("update available resolution")
    inner class UpdateResolutionAvailable {

      private lateinit var messageAttachment: MessageAttachment

      @BeforeEach
      fun setup() {
        messageAttachment = spyk(attachment().buildMessageAttachment())
        every { attachmentRepository.findAttachmentByIdentifier(any()) } returns
            (messageAttachment as Attachment<*, *>?)

        every { attachmentRepository.save(any(), any()) } answers
            {
              it.invocation.args.first() as Attachment<*, *>
            }
      }

      @Test
      @DisplayName("without updating file size and width/height")
      fun updatesAttachment() {
        cut.updateImageMetadata(randomUUID(), 1000L, null, UPDATED)

        verify { messageAttachment.setResolutionAvailable(SMALL) }
        verify { messageAttachment.setResolutionAvailable(FULL) }

        verify(exactly = 0) { messageAttachment.fileSize }
        verify(exactly = 0) { messageAttachment.imageHeight }
        verify(exactly = 0) { messageAttachment.imageWidth }
      }
    }
  }
}
