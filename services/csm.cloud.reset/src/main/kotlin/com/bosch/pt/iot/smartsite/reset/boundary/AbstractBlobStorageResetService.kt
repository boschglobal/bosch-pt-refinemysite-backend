/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.BlobItem
import java.util.function.Consumer
import org.slf4j.LoggerFactory

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractBlobStorageResetService(private val blobServiceClient: BlobServiceClient) {

  protected fun getContainerReference(containerName: String): BlobContainerClient? =
      try {
        blobServiceClient.getBlobContainerClient(containerName)
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        LOGGER.error("Error connecting to azure storage account blob container $containerName", e)
        null
      }

  protected fun exists(containerClient: BlobContainerClient): Boolean {
    return try {
      if (!containerClient.exists()) {
        LOGGER.info("No such storage container: {}", containerClient.blobContainerName)
        return false
      }
      true
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      LOGGER.error("Error checking existence of container " + containerClient.blobContainerName, e)
      false
    }
  }

  protected fun deleteAllBlobs(containerClient: BlobContainerClient) =
      try {
        LOGGER.info("Deleting blobs in container {}...", containerClient.blobContainerName)
        containerClient.listBlobs().forEach(Consumer { deleteBlob(containerClient, it) })
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        LOGGER.error(
            "Error deleting all blobs from container ${containerClient.blobContainerName}", e)
      }

  private fun deleteBlob(containerClient: BlobContainerClient, blob: BlobItem) {
    try {
      containerClient.getBlobClient(blob.name).delete()
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      LOGGER.error("Error deleting blob ${blob.name}", e)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractBlobStorageResetService::class.java)
  }
}
