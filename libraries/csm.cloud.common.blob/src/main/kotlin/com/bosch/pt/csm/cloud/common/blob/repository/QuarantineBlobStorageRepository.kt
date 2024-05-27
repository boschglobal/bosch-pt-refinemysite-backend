/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.repository

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.Companion.KEY_FILENAME
import com.bosch.pt.csm.cloud.common.blob.model.NewQuarantineBlob
import com.bosch.pt.csm.cloud.common.config.QuarantineBlobStorageProperties
import com.azure.core.util.polling.LongRunningOperationStatus
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.specialized.BlockBlobClient
import generateSignedUrlWithPermissions
import java.io.ByteArrayInputStream
import java.time.Duration
import java.util.Locale
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Autoconfigured by [com.bosch.pt.csm.cloud.common.config.QuarantineBlobStorageAutoConfiguration].
 */
open class QuarantineBlobStorageRepository(
    private val quarantineBlobStorageProperties: QuarantineBlobStorageProperties,
    @Qualifier("quarantineBlobContainerClient") val quarantineClient: BlobContainerClient?,
) : AbstractAzureBlobStorageRepository() {

  open fun save(data: ByteArray, blobName: String, mimeType: String, metadata: BlobMetadata): Blob {
    require(blobName.isNotBlank()) { "blobName may not be empty" }
    require(mimeType.isNotBlank()) { "mimeType may not be empty" }

    val containerName = quarantineBlobStorageProperties.containerName
    val directory = quarantineBlobStorageProperties.directory

    return trace(containerName, directory, "upload to blob storage") { traceHeader ->
      ByteArrayInputStream(data).use {
        metadata.put("trace_header", traceHeader)
        if (metadata.containsKey(KEY_FILENAME)) {
          metadata.put(
              KEY_FILENAME, metadata.get(KEY_FILENAME)!!.replace("[^a-zA-Z0-9.]+".toRegex(), "_"))
        }

        // Blog http headers
        val blobHttpHeaders = BlobHttpHeaders().setContentType(mimeType)
        if (metadata.containsKey(KEY_FILENAME)) {
          blobHttpHeaders.contentDisposition =
              String.format(
                  Locale.getDefault(), "attachment; filename=\"%s\"", metadata.get(KEY_FILENAME))
        } else {
          blobHttpHeaders.contentDisposition = "attachment"
        }

        // Blob metadata
        val blobMetadata = HashMap(metadata.toMap())

        // Upload
        getQuarantineBlobClient(blobName).apply {
          uploadWithResponse(
              it, data.size.toLong(), blobHttpHeaders, blobMetadata, null, null, null, null, null)
        }
        return@trace Blob(blobName, data, metadata, mimeType)
      }
    }
  }

  /**
   * Generate an upload URL which can be used to start an upload within the configured time interval
   * for expiration. This upload URL can safely be shared with frontend clients for user uploads.
   * The uploads will be checked for malware by the system.
   */
  open fun generateSignedUploadUrl(blobName: String) =
      generateSignedUploadUrl(
          blobName, quarantineBlobStorageProperties.sharedAccessExpiryTime.toLong())

  /**
   * Generate an upload URL which can be used to start an upload within the specified time interval
   * for expiration. This upload URL can safely be shared with frontend clients for user uploads.
   * The uploads will be checked for malware by the system.
   */
  open fun generateSignedUploadUrl(blobName: String, expiryInSeconds: Long): NewQuarantineBlob {
    require(expiryInSeconds > 0) { "expiryInSeconds must be larger than 0" }

    val versionId = UUID.randomUUID().toString()
    val quarantineBlobName = "${quarantineBlobStorageProperties.directory}/$blobName/$versionId"
    val uploadUrl =
        getQuarantineBlobClient(quarantineBlobName)
            .generateSignedUrlWithPermissions(WRITE_PERMISSION, expiryInSeconds)
    return NewQuarantineBlob(uploadUrl, quarantineBlobName, versionId)
  }

  /**
   * Copies the specified blob from the quarantine storage to the target storage (the "normal"
   * storage). When copying the last path segment of the blob name is removed as it's the random
   * identifier added in generateSignedUploadUrl.
   *
   * @return blob name in target storage
   */
  open fun moveFromQuarantine(
      quarantineBlobName: String,
      targetBlobContainerClient: BlobContainerClient,
      removeVersionIdSuffix: Boolean = false,
      removeQuarantineDirectoryPrefix: Boolean = true,
  ): String {
    val sourceBlockBlobClient = getQuarantineBlobClient(quarantineBlobName)
    require(sourceBlockBlobClient.exists()) {
      "blob '$quarantineBlobName' does not exist in quarantine"
    }

    val targetBlobName =
        quarantineBlobName
            .split("/")
            .filter { it.isNotBlank() }
            .toMutableList()
            .apply {
              if (removeQuarantineDirectoryPrefix) removeFirst()
              if (removeVersionIdSuffix) removeLast()
            }
            .joinToString("/")

    val sourceDownloadUrl =
        sourceBlockBlobClient
            .generateSignedUrlWithPermissions(
                READ_PERMISSION, quarantineBlobStorageProperties.sharedAccessExpiryTime.toLong())
            .toString()

    val targetBlockBlobClient =
        targetBlobContainerClient.getBlobClient(targetBlobName).blockBlobClient
    val poller = targetBlockBlobClient.beginCopy(sourceDownloadUrl, Duration.ofSeconds(2))
    val response = poller.waitForCompletion()

    if (response.status != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED)
        error(
            "Could not copy from quarantine, status: ${response.status}, error: ${response.value.error}")

    return targetBlobName
  }

  private fun getQuarantineBlobClient(blobName: String): BlockBlobClient {
    check(quarantineClient != null) {
      "Quarantine StorageAccount not configured, is 'custom.quarantine-storage.connection-string' configured?"
    }
    return quarantineClient.getBlobClient(blobName).blockBlobClient
  }

  companion object {
    private const val READ_PERMISSION = "r"
    private const val WRITE_PERMISSION = "w"
  }
}
