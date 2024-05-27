/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.repository

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.iot.smartsite.application.config.QuarantineBlobStorageProperties
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.Companion.MAX_RETIRES_MALWARE_SCAN
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.NOT_SCANNED
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.SAFE
import com.azure.core.util.polling.LongRunningOperationStatus.SUCCESSFULLY_COMPLETED
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.specialized.BlockBlobClient
import datadog.trace.api.Trace
import generateSignedUrlWithPermissions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Boolean.TRUE
import java.time.Duration
import java.util.UUID
import java.util.UUID.randomUUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

@Component
class ImportBlobStorageRepository(
    @Qualifier("importContainerClient") private val blobContainerClient: BlobContainerClient,
    @Qualifier("importQuarantineContainerClient")
    private val quarantineContainerClient: BlobContainerClient,
    private val quarantineProperties: QuarantineBlobStorageProperties,
    private val environment: Environment
) {

  fun save(
      file: ByteArray,
      fileName: String,
      mimeType: String,
      metadata: MutableMap<String, String>
  ): UUID = randomUUID().also { save(generateBlobName(it), file, fileName, mimeType, metadata) }

  @Trace
  fun save(
      blobName: String,
      file: ByteArray,
      fileName: String,
      mimeType: String,
      metadata: MutableMap<String, String>
  ): Blob {
    val fileNameSanitized = fileName.replace("[^a-zA-Z0-9.]+".toRegex(), "_")
    metadata["filename"] = fileNameSanitized
    try {
      // append imports directory
      val blockBlobClient =
          quarantineContainerClient.getBlobClient("imports/$blobName").blockBlobClient
      val blobHttpHeaders =
          BlobHttpHeaders()
              .setContentType(mimeType)
              .setContentDisposition("attachment; filename=\"$fileNameSanitized\"")
      blockBlobClient.uploadWithResponse(
          ByteArrayInputStream(file),
          file.size.toLong(),
          blobHttpHeaders,
          metadata,
          null,
          null,
          null,
          null,
          null)
    } catch (e: IOException) {
      throw IllegalStateException(e)
    }

    return Blob(blobName, file, BlobMetadata.fromMap(metadata), mimeType)
  }

  @Trace
  fun moveFromQuarantine(
      blobName: String,
  ) {

    val sourceBlockBlobClient =
        quarantineContainerClient.getBlobClient("imports/$blobName").blockBlobClient

    require(sourceBlockBlobClient.exists()) { "blob '$blobName' does not exist in quarantine" }

    val sourceDownloadUrl =
        sourceBlockBlobClient
            .generateSignedUrlWithPermissions(
                "r", quarantineProperties.sharedAccessExpiryTimeSeconds)
            .toString()

    val targetBlockBlobClient = blobContainerClient.getBlobClient(blobName).blockBlobClient
    val poller = targetBlockBlobClient.beginCopy(sourceDownloadUrl, Duration.ofSeconds(1))
    val response = poller.waitForCompletion()

    if (response.status != SUCCESSFULLY_COMPLETED)
        error(
            "Could not copy from quarantine, status: ${response.status}, error: ${response.value.error}")

    // delete source file if import was moved to correct storage account
    sourceBlockBlobClient.deleteIfExists()
  }

  @Trace
  fun getMalwareScanResult(blobName: String): MalwareScanResult {
    // Ignore Malware scanning locally, it's not required
    if (environment.acceptsProfiles(Profiles.of("local"))) return SAFE

    val scanResult =
        quarantineContainerClient
            .getBlobClient("imports/$blobName")
            .tags[MALWARE_SCANNING_RESULT_KEY]
    return MalwareScanResult.fromText(scanResult)
  }

  @Trace
  fun getMalwareScanResultBlocking(blobName: String): MalwareScanResult {
    var retryCount = 0
    var result: MalwareScanResult = getMalwareScanResult(blobName)

    while (result == NOT_SCANNED && retryCount < MAX_RETIRES_MALWARE_SCAN) {
      delayMalwareScanResultCheck()
      retryCount++
      result = getMalwareScanResult(blobName)
    }

    return result
  }

  fun delayMalwareScanResultCheck() {
    Thread.sleep(Duration.ofSeconds(1).toMillis())
  }

  @Trace
  fun find(blobName: String): Blob? {
    try {
      ByteArrayOutputStream().use { out ->
        val blockBlobClient = blobContainerClient.getBlobClient(blobName)
        if (!blockBlobClient.exists()) {
          return null
        }
        blockBlobClient.downloadStream(out)

        val metadata = BlobMetadata.fromMap(blockBlobClient.properties.metadata)
        return Blob(blobName, out.toByteArray(), metadata, blockBlobClient.properties.contentType)
      }
    } catch (e: IOException) {
      LOGGER.error("Failed to download Blob {}: {}", blobName, e.message)
      return null
    }
  }

  fun read(blobName: String): InputStream? =
      blobContainerClient.getBlobClient(blobName).blockBlobClient.let {
        if (it.exists()) it.openInputStream() else null
      }

  @Trace
  fun deleteIfExists(blobName: String): Boolean =
      blobContainerClient.getBlobClient(blobName).blockBlobClient?.let { deleteBlobIfExists(it) }
          ?: false

  private fun deleteBlobIfExists(blockBlobClient: BlockBlobClient): Boolean =
      if (TRUE == blockBlobClient.exists()) {
        blockBlobClient.delete()
        LOGGER.info("Blob {} deleted", blockBlobClient.blobName)
        true
      } else {
        LOGGER.info("Blob {} does not exist", blockBlobClient.blobName)
        false
      }

  private fun generateBlobName(identifier: UUID) =
      "${SecurityContextHelper.getInstance().getCurrentUser().identifier}/$identifier}"

  companion object {
    const val MALWARE_SCANNING_RESULT_KEY = "Malware Scanning scan result"

    private val LOGGER = LoggerFactory.getLogger(ImportBlobStorageRepository::class.java)
  }
}

enum class MalwareScanResult(val text: String) {
  NOT_SCANNED(""),
  SAFE("No threats found"),
  MALICIOUS("Malicious");

  companion object {
    const val MAX_RETIRES_MALWARE_SCAN = 30

    fun fromText(text: String?): MalwareScanResult =
        MalwareScanResult.values().firstOrNull { it.text == text } ?: NOT_SCANNED
  }
}
