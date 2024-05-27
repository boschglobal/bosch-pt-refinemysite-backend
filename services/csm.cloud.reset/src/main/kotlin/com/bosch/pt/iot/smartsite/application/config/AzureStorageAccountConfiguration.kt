/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class AzureStorageAccountConfiguration(
    @Value("\${custom.blob-storage-project-images.connection-string:}")
    val connectionStringAzureAccountProjectService: String,
    @Value("\${custom.blob-storage-project-downloads.connection-string:}")
    val connectionStringAzureAccountProjectServiceDownloads: String,
    @Value("\${custom.blob-storage-user-images.connection-string:}")
    val connectionStringAzureAccountUserService: String,
    @Value("\${custom.blob-storage-testdata-download.connection-string:}")
    val connectionStringAzureAccountResetService: String
) {

  @Bean
  fun blobServiceClientForProjectServiceImages(): BlobServiceClient {
    LOGGER.info("Initializing connection to azure storage account for project service images...")
    return BlobServiceClientBuilder()
        .connectionString(connectionStringAzureAccountProjectService)
        .buildClient()
  }

  @Bean
  fun blobServiceClientForProjectServiceDownloads(): BlobServiceClient {
    LOGGER.info("Initializing connection to azure storage account for project service downloads...")
    return BlobServiceClientBuilder()
        .connectionString(connectionStringAzureAccountProjectServiceDownloads)
        .buildClient()
  }

  @Bean
  fun blobServiceClientForUserServiceImages(): BlobServiceClient =
      try {
        LOGGER.info("Initializing connection to azure storage account for user service...")
        BlobServiceClientBuilder()
            .connectionString(connectionStringAzureAccountUserService)
            .buildClient()
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw IllegalStateException(e)
      }

  @Bean
  @Profile("azure-blob-download")
  fun blobServiceClientForTestDataDownload(): BlobServiceClient =
      try {
        LOGGER.info("Initializing connection to azure storage account for test data download...")
        BlobServiceClientBuilder()
            .connectionString(connectionStringAzureAccountResetService)
            .buildClient()
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw IllegalStateException(e)
      }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AzureStorageAccountConfiguration::class.java)
  }
}
