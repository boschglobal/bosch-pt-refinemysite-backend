/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_WRITING_OPERATIONS
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.attachment.util.MimeTypeDetector
import com.bosch.pt.csm.cloud.usermanagement.user.picture.command.snapshotstore.ProfilePictureSnapshot
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.model.ProfilePicture
import java.io.InputStream
import java.net.URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
class BlobStoreService(
    private val blobStorageProperties: BlobStorageProperties,
    private val blobRepository: AzureBlobStorageRepository,
    private val quarantineBlobStorageRepository: QuarantineBlobStorageRepository,
    private val mimeTypeDetector: MimeTypeDetector,
    @Value("\${block-modifying-operations:false}")
    private val blockModifyingOperations: Boolean = false
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
  fun saveImage(data: ByteArray, blobOwner: BlobOwner, blobMetadata: BlobMetadata): Blob {
    assertModifyingOperationsNotBlocked()
    val mimeType = mimeTypeDetector.detect(data)
    val blobName = buildQuarantineBlobName(blobOwner)
    return quarantineBlobStorageRepository.save(data, blobName, mimeType, blobMetadata)
  }

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY, readOnly = true)
  fun read(blobName: String): InputStream? = blobRepository.read(blobName)

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY, readOnly = true)
  fun generateSignedUrlForImage(blobOwner: ImageBlobOwner, imageResolution: ImageResolution): URL? =
      blobRepository.generateSignedUrl(buildBlobName(blobOwner, imageResolution))

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  fun deleteImageIfExists(blobOwner: BlobOwner, imageResolution: ImageResolution) {
    assertModifyingOperationsNotBlocked()
    blobRepository.deleteIfExists(buildBlobName(blobOwner, imageResolution))
  }

  private fun buildBlobName(blobOwner: BlobOwner, imageResolution: ImageResolution): String =
      listOf(
              blobOwner.getBoundedContext().name.lowercase(),
              blobStorageProperties.getImageFolder(imageResolution),
              blobOwner.getParentIdentifier().toString(),
              blobOwner.getIdentifierUuid().toString())
          .joinToString(FOLDER_SEPARATOR)

  private fun buildQuarantineBlobName(blobOwner: BlobOwner): String =
      when (blobOwner) {
        is ProfilePicture -> userImageBlobName(blobOwner)
        is ProfilePictureSnapshot -> userImageBlobName(blobOwner)
        else -> error("Unsupported attachment type found: ${blobOwner.getOwnerType()}")
      }

  private fun userImageBlobName(blobOwner: BlobOwner) =
      arrayOf(
              "images/users",
              blobOwner.getParentIdentifier().toString(),
              "picture",
              blobOwner.getIdentifierUuid().toString())
          .joinToString(separator = FOLDER_SEPARATOR)

  private fun assertModifyingOperationsNotBlocked() {
    if (blockModifyingOperations) {
      throw BlockOperationsException(BLOCK_WRITING_OPERATIONS)
    }
  }

  companion object {
    private const val FOLDER_SEPARATOR = "/"
  }
}
