/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.service

import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import java.lang.String.join
import java.net.URL
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class BlobStoreService(
    // Error just in Intellij
    private val blobStorageProperties: BlobStorageProperties,
    private val azureBlobStorageRepository: AzureBlobStorageRepository
) {

  fun blobName(
      boundedContext: BoundedContext,
      parentIdentifier: UUID,
      attachmentIdentifier: UUID,
      imageResolution: ImageResolution
  ): String {
    return join(
        FOLDER_SEPARATOR,
        boundedContext.name.lowercase(),
        blobStorageProperties.getImageFolder(imageResolution),
        parentIdentifier.toString(),
        attachmentIdentifier.toString())
  }

  fun generateSignedUrlForImage(
      boundedContext: BoundedContext,
      parentIdentifier: UUID,
      attachmentIdentifier: UUID,
      imageResolution: ImageResolution
  ): URL? {
    val blobName = blobName(boundedContext, parentIdentifier, attachmentIdentifier, imageResolution)

    // Get signed URL of requested size
    var signedUrl = azureBlobStorageRepository.generateSignedUrl(blobName)
    // If the requested size isn't available yet, return a signed url to the original size
    if (signedUrl == null) {
      signedUrl =
          azureBlobStorageRepository.generateSignedUrl(
              blobName(
                  boundedContext, parentIdentifier, attachmentIdentifier, ImageResolution.ORIGINAL))
    }
    return signedUrl
  }

  companion object {
    private const val FOLDER_SEPARATOR = "/"
  }
}
