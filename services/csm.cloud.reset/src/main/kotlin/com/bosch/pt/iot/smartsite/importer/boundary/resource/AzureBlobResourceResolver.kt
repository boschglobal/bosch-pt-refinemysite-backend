package com.bosch.pt.iot.smartsite.importer.boundary.resource

import com.azure.storage.blob.BlobServiceClient
import java.io.File
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
@Profile("azure-blob-download")
class AzureBlobResourceResolver(
    @Qualifier("blobServiceClientForTestDataDownload")
    private val blobServiceClient: BlobServiceClient
) : BlobResourceResolver {

  override fun getBlobResource(filepath: String): Resource? {
    val absoluteFilePath = LOCAL_PATH_PREFIX + filepath
    var fileSystemResource = FileSystemResource(absoluteFilePath)

    if (!fileSystemResource.exists()) {
      LOGGER.info("Downloading blob resource from Azure: " + fileSystemResource.filename)

      try {
        val file = File(absoluteFilePath)
        file.parentFile.mkdirs()

        blobServiceClient
            .getBlobContainerClient(BLOB_CONTAINER_NAME)
            .getBlobClient(filepath)
            .downloadToFile(absoluteFilePath)
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        LOGGER.error("Error while downloading blob from azure storage: $e")
        return null
      }
      fileSystemResource = FileSystemResource(absoluteFilePath)
    } else {
      LOGGER.info("Using locally cached blob resource: " + fileSystemResource.filename)
    }

    return fileSystemResource
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AzureBlobResourceResolver::class.java)

    private const val BLOB_CONTAINER_NAME = "pt-csm-testdata"
    private const val LOCAL_PATH_PREFIX = "/tmp/datasets/"
  }
}
