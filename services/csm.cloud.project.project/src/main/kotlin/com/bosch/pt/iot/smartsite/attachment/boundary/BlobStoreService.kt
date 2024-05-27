/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.util.MimeTypeDetector
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import java.io.InputStream
import java.net.URL
import java.util.Locale
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional

@Service
open class BlobStoreService(
    private val blobStorageProperties: BlobStorageProperties,
    private val blobRepository: AzureBlobStorageRepository,
    private val quarantineBlobStorageRepository: QuarantineBlobStorageRepository,
    private val mimeTypeDetector: MimeTypeDetector,
    @param:Value("\${block-modifying-operations:false}")
    private val blockModifyingOperations: Boolean
) {

  /**
   * Stores an image [Blob] belonging to the given Blob owner.
   *
   * @param data the data to be stored
   * @param blobOwner the blob owner
   * @param blobMetadata additional information on the blob
   * @return the stored [Blob]
   */
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun saveImage(data: ByteArray, blobOwner: BlobOwner, blobMetadata: BlobMetadata): Blob {
    assertModifyingOperationsNotBlocked()
    val mimeType = mimeTypeDetector.detect(data)
    val blobName = buildQuarantineBlobName(blobOwner)
    return quarantineBlobStorageRepository.save(data, blobName, mimeType, blobMetadata)
  }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun generateSignedUrlForImage(
      blobOwner: ImageBlobOwner,
      imageResolution: ImageResolution
  ): URL? = blobRepository.generateSignedUrl(buildBlobName(blobOwner, imageResolution))

  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun openImage(blobOwner: ImageBlobOwner, imageResolution: ImageResolution): InputStream? =
      blobRepository.read(buildBlobName(blobOwner, imageResolution))

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteImageIfExists(blobOwner: BlobOwner, imageResolution: ImageResolution) {
    assertModifyingOperationsNotBlocked()
    blobRepository.deleteIfExists(buildBlobName(blobOwner, imageResolution))
  }

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteAllResolutionsOfImageIfExists(blobOwner: BlobOwner) {
    assertModifyingOperationsNotBlocked()
    ImageResolution.values().forEach { resolution: ImageResolution ->
      blobRepository.deleteIfExists(buildBlobName(blobOwner, resolution))
    }
  }

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteImagesInDirectory(directoryName: String) {
    assertModifyingOperationsNotBlocked()
    require(directoryName.isNotBlank()) { "directoryName must not be empty" }

    ImageResolution.values().forEach { resolution: ImageResolution ->
      blobRepository.deleteBlobsInDirectory(getDirectoryName(directoryName, resolution))
    }
  }

  @NoPreAuthorize
  @Transactional(propagation = SUPPORTS)
  open fun getDirectoryName(directoryName: String, resolution: ImageResolution): String =
      arrayOf(
              BoundedContext.PROJECT.name.lowercase(Locale.getDefault()),
              blobStorageProperties.getImageFolder(resolution),
              directoryName)
          .joinToString(separator = FOLDER_SEPARATOR) + FOLDER_SEPARATOR

  @NoPreAuthorize
  @Transactional(propagation = SUPPORTS)
  open fun buildBlobName(blobOwner: BlobOwner, imageResolution: ImageResolution): String =
      arrayOf(
              blobOwner.getBoundedContext().name.lowercase(Locale.getDefault()),
              blobStorageProperties.getImageFolder(imageResolution),
              blobOwner.getParentIdentifier().toString(),
              blobOwner.getIdentifierUuid().toString())
          .joinToString(separator = FOLDER_SEPARATOR)

  private fun buildQuarantineBlobName(blobOwner: BlobOwner): String =
      when (blobOwner) {
        is ProjectPicture ->
            arrayOf(
                    "images/projects",
                    blobOwner.getParentIdentifier().toString(),
                    "picture",
                    blobOwner.getIdentifierUuid().toString())
                .joinToString(separator = FOLDER_SEPARATOR)
        is TaskAttachment ->
            arrayOf(
                    "images/projects",
                    blobOwner.task!!.project.identifier,
                    "tasks",
                    blobOwner.task!!.identifier,
                    blobOwner.getIdentifierUuid().toString())
                .joinToString(separator = FOLDER_SEPARATOR)
        is TopicAttachment ->
            arrayOf(
                    "images/projects",
                    blobOwner.task!!.project.identifier,
                    "tasks",
                    blobOwner.task!!.identifier,
                    "topics",
                    blobOwner.topic!!.identifier,
                    blobOwner.getIdentifierUuid().toString())
                .joinToString(separator = FOLDER_SEPARATOR)
        is MessageAttachment ->
            arrayOf(
                    "images/projects",
                    blobOwner.task!!.project.identifier,
                    "tasks",
                    blobOwner.task!!.identifier,
                    "topics",
                    blobOwner.topic!!.identifier,
                    "messages",
                    blobOwner.message!!.identifier,
                    blobOwner.getIdentifierUuid().toString())
                .joinToString(separator = FOLDER_SEPARATOR)
        else -> error("Unsupported attachment type found: ${blobOwner.getOwnerType()}")
      }

  private fun assertModifyingOperationsNotBlocked() {
    if (blockModifyingOperations) {
      throw BlockOperationsException(BLOCK_WRITING_OPERATIONS)
    }
  }

  companion object {
    private const val FOLDER_SEPARATOR = "/"
  }
}
