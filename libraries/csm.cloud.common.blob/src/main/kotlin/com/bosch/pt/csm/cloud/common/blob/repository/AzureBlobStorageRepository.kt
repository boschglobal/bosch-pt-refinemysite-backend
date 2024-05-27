/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.repository

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobListDetails
import com.azure.storage.blob.models.ListBlobsOptions
import com.azure.storage.blob.specialized.BlockBlobClient
import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.Companion.fromMap
import com.bosch.pt.csm.cloud.common.blob.model.BlobProperties
import com.bosch.pt.csm.cloud.common.blob.model.MalwareScanResult
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import generateSignedUrlWithPermissions
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.function.Consumer
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

/** Autoconfigured by [com.bosch.pt.csm.cloud.common.config.AzureBlobStorageAutoConfiguration]. */
open class AzureBlobStorageRepository(
    val blobContainerClient: BlobContainerClient,
    private val blobStorageProperties: BlobStorageProperties,
    private val environment: Environment
) {

  @ExcludeFromCodeCoverage
  open fun find(blobName: String): Blob? {
    require(blobName.isNotBlank()) { "blobName may not be empty" }
    try {
      ByteArrayOutputStream().use {
        val blockBlobClient = getBlockBlobClient(blobName)
        return if (!blockBlobClient.exists()) {
          // Blob does not exist
          null
        } else {
          val blobProperties = blockBlobClient.properties
          blockBlobClient.downloadStream(it)
          val metadata: BlobMetadata = fromMap(blockBlobClient.properties.metadata)
          Blob(blobName, it.toByteArray(), metadata, blobProperties.contentType)
        }
      }
    } catch (e: IOException) {
      LOGGER.error("Failed to download Blob {}: {}", blobName, e.message)
      return null
    }
  }

  open fun findProperties(blobName: String): BlobProperties {
    val blockBlobClient = getBlockBlobClient(blobName)
    return BlobProperties(blobSize = blockBlobClient.properties.blobSize)
  }

  open fun getMalwareScanResult(blobName: String): MalwareScanResult {
    val scanResult = getBlockBlobClient(blobName).tags[MALWARE_SCANNING_RESULT_KEY]
    return MalwareScanResult.fromText(scanResult)
  }

  open fun read(blobName: String): InputStream? =
      getBlockBlobClient(blobName).let { if (it.exists()) it.openInputStream() else null }

  @ExcludeFromCodeCoverage
  open fun deleteIfExists(blobName: String): Boolean {
    require(blobName.isNotBlank()) { "blobName may not be empty" }
    val blockBlobClient = getBlockBlobClient(blobName)
    return deleteBlobIfExists(blockBlobClient)
  }

  /** Deletes all blobs within a directory except for the blob with name [excludedBlobName] */
  @ExcludeFromCodeCoverage
  open fun deleteBlobsInDirectory(directoryName: String, excludedBlobName: String? = null) {
    require(directoryName.isNotBlank()) { "directoryName may not be empty" }
    val options = ListBlobsOptions().setPrefix(directoryName)
    // this is done to prevent a BadRequest (400) from the azurite container used locally
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      options.details = BlobListDetails().setRetrieveDeletedBlobs(true)
    }
    LOGGER.info("Deleting blobs in folder {} ...", directoryName)
    blobContainerClient
        .listBlobsByHierarchy("/", options, null)
        ?.filter { it.name != excludedBlobName }
        ?.forEach(Consumer { blob: BlobItem -> deleteBlobIfExists(getBlockBlobClient(blob.name)) })
  }

  @ExcludeFromCodeCoverage
  private fun deleteBlobIfExists(blockBlobClient: BlockBlobClient): Boolean {
    return if (blockBlobClient.exists()) {
      blockBlobClient.delete()
      LOGGER.info("Blob {} deleted", blockBlobClient.blobName)
      true
    } else {
      LOGGER.info("Blob {} does not exist", blockBlobClient.blobName)
      false
    }
  }

  open fun generateSignedUrl(blobName: String) =
      generateSignedUrl(blobName, blobStorageProperties.sharedAccessExpiryTime.toLong())

  open fun generateSignedUrl(blobName: String, expiryInSeconds: Long): URL? {
    require(expiryInSeconds > 0) { "expiryInSeconds must be larger than 0" }
    return getBlockBlobClient(blobName).let {
      if (it.exists()) it.generateSignedUrlWithPermissions(READ_PERMISSION, expiryInSeconds)
      else null
    }
  }

  /**
   * Generate a URL including a SaS Token to write to the blob storage. This is not meant to be
   * exposed to anyone besides our own applications! User uploads should use the
   * [QuarantineBlobStorageRepository]. The token expires after the configured expiration time
   * interval.
   */
  open fun generateSignedUploadUrlForInternalUse(blobName: String) =
      generateSignedUploadUrlForInternalUse(
          blobName, blobStorageProperties.sharedAccessExpiryTime.toLong())

  /**
   * Generate a URL including a SaS Token to write to the blob storage. This is not meant to be
   * exposed to anyone besides our own applications! User uploads should use the
   * [QuarantineBlobStorageRepository]. The token expires after the specified expiration time
   * interval.
   */
  open fun generateSignedUploadUrlForInternalUse(blobName: String, expiryInSeconds: Long): URL {
    require(expiryInSeconds > 0) { "expiryInSeconds must be larger than 0" }
    return getBlockBlobClient(blobName)
        .generateSignedUrlWithPermissions(WRITE_PERMISSION, expiryInSeconds)
  }

  @ExcludeFromCodeCoverage
  fun getBlockBlobClient(blobName: String): BlockBlobClient =
      blobContainerClient.getBlobClient(blobName).blockBlobClient

  companion object {
    private val LOGGER = getLogger(AzureBlobStorageRepository::class.java)
    private const val READ_PERMISSION = "r"
    private const val WRITE_PERMISSION = "w"
    private const val MALWARE_SCANNING_RESULT_KEY = "Malware Scanning scan result"
  }
}
