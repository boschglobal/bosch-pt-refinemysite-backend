/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import datadog.trace.api.Trace
import java.io.InputStream
import java.net.URL
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class AttachmentService(
    private val blobStoreService: BlobStoreService,
    private val attachmentRepository: AttachmentRepository
) {

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun updateImageMetadata(
      attachmentIdentifier: UUID,
      fileSize: Long,
      imageMetadata: ImageMetadataDto?,
      eventType: Enum<*>
  ): Attachment<*, *> =
      findAttachmentOrThrowException(attachmentIdentifier)
          .apply {
            this.fileSize = fileSize
            this.captureDate = imageMetadata?.imageCreationDate
            this.imageHeight = imageMetadata?.imageHeight ?: 0
            this.imageWidth = imageMetadata?.imageWidth ?: 0
            setResolutionAvailable(ORIGINAL)
            setResolutionAvailable(FULL)
            setResolutionAvailable(SMALL)
          }
          .also { attachmentRepository.save(it, eventType) }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun storeBlob(data: ByteArray, attachment: Attachment<*, *>, metadata: BlobMetadata): Blob =
      blobStoreService.saveImage(data, attachment, metadata)

  @Trace
  @PreAuthorize(
      "@attachmentAuthorizationComponent.hasViewPermissionOnAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  open fun generateBlobAccessUrl(
      attachmentIdentifier: UUID,
      attachmentImageResolution: AttachmentImageResolution
  ): URL {
    val attachment = findAttachmentOrThrowException(attachmentIdentifier)
    val imageResolution =
        attachment.getResolutionOrOriginal(attachmentImageResolution.imageResolution)
    return blobStoreService.generateSignedUrlForImage(attachment, imageResolution)
        ?: throw AggregateNotFoundException(
            ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())
  }

  @Trace
  @PreAuthorize(
      "@attachmentAuthorizationComponent.hasViewPermissionOnAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  open fun openAttachment(
      attachmentIdentifier: UUID,
      attachmentImageResolution: AttachmentImageResolution
  ): InputStream {
    val attachment = findAttachmentOrThrowException(attachmentIdentifier)
    val imageResolution =
        attachment.getResolutionOrOriginal(attachmentImageResolution.imageResolution)
    return blobStoreService.openImage(attachment, imageResolution)
        ?: throw AggregateNotFoundException(
            ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAttachment(attachmentIdentifier: UUID): Attachment<*, *>? =
      attachmentRepository.findAttachmentByIdentifier(attachmentIdentifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAttachmentOrThrowException(attachmentIdentifier: UUID): Attachment<*, *> =
      attachmentRepository.findAttachmentByIdentifier(attachmentIdentifier)
          ?: throw AggregateNotFoundException(
              ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteAllResolutionsOfImageIfExists(blobOwner: BlobOwner) =
      blobStoreService.deleteAllResolutionsOfImageIfExists(blobOwner)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun deleteAttachmentIfExists(attachmentIdentifier: UUID, eventType: Enum<*>) =
      attachmentRepository.findAttachmentByIdentifier(attachmentIdentifier)?.apply {
        deleteAllResolutionsOfImageIfExists(this)
        attachmentRepository.delete(this, eventType)
      }
}
