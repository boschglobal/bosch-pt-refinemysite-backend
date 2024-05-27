/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.download.repository

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.application.config.DownloadBlobStorageProperties
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobHttpHeaders
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.sas.BlobContainerSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Boolean.FALSE
import java.net.MalformedURLException
import java.net.URL
import java.time.OffsetDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class DownloadBlobStorageRepository(
    @Qualifier("downloadContainerClient") private val blobContainerClient: BlobContainerClient,
    private val properties: DownloadBlobStorageProperties,
) {

  @ExcludeFromCodeCoverage
  fun save(
      fileContent: ByteArray,
      fileName: String,
      mimeType: String,
      metadata: Map<String, String>
  ): UUID {
    return uploadBlobStream(fileName, mimeType, metadata) { os ->
      fileContent.inputStream().use { it.copyTo(os, BUFFER_SIZE) }
    }
  }

  fun uploadBlobStream(
      fileName: String,
      mimeType: String,
      metadata: Map<String, String>,
      uploadBlock: (OutputStream) -> Unit
  ): UUID {
    val documentId = randomUUID()
    val fileNameSanitized = fileName.replace("[^a-zA-Z0-9.]+".toRegex(), "_")
    try {
      val blockBlobClient =
          blobContainerClient.getBlobClient(generateBlobName(documentId)).blockBlobClient
      BufferedOutputStream(blockBlobClient.blobOutputStream, BUFFER_SIZE).use { bos ->
        uploadBlock.invoke(bos)
      }
      blockBlobClient.setHttpHeaders(
          BlobHttpHeaders()
              .setContentType(mimeType)
              .setContentDisposition("attachment; filename=\"$fileNameSanitized\""))
      blockBlobClient.setMetadata(metadata + ("filename" to fileNameSanitized))
    } catch (e: IOException) {
      error(e)
    } catch (e: BlobStorageException) {
      error(e)
    }
    return documentId
  }

  @ExcludeFromCodeCoverage
  fun generateSignedUrl(documentId: UUID): URL {
    val blockBlobClient =
        blobContainerClient.getBlobClient(generateBlobName(documentId)).blockBlobClient
    if (FALSE == blockBlobClient.exists()) {
      throw IllegalStateException("Blob does not exists")
    }
    return try {
      OffsetDateTime.now()
          .plusSeconds(properties.sharedAccessExpiryTimeSeconds)
          .let { BlobServiceSasSignatureValues(it, BlobContainerSasPermission.parse("r")) }
          .let { blockBlobClient.generateSas(it) }
          .let { URL("${blockBlobClient.blobUrl}?$it") }
    } catch (e: MalformedURLException) {
      throw IllegalStateException(e)
    }
  }

  private fun generateBlobName(identifier: UUID) =
      "${SecurityContextHelper.getInstance().getCurrentUser().identifier}/$identifier}"

  companion object {
    /**
     * Stream buffer size: needs to be reasonably high for upload speed to the blob store, but not
     * too high to avoid memory pressure on the service. 4MB is enough to upload most attachments in
     * a single step.
     */
    const val BUFFER_SIZE = 4 * 1024 * 1024 // 4MB
  }
}
