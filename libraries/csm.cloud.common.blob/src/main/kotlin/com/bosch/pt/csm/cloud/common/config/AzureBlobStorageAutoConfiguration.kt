/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.config

import com.azure.core.util.Context
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED

@AutoConfiguration
@ConditionalOnProperty("custom.blob-storage.connection-string")
@EnableConfigurationProperties(BlobStorageProperties::class)
open class AzureBlobStorageAutoConfiguration(
    @Value("\${custom.blob-storage.connection-string}") private val connectionString: String,
    private val environment: Environment
) {

  @Bean
  open fun azureBlobStorageRepository(
      blobContainerClient: BlobContainerClient,
      blobStorageProperties: BlobStorageProperties,
      environment: Environment
  ) = AzureBlobStorageRepository(blobContainerClient, blobStorageProperties, environment)

  @Bean
  @Primary
  open fun azureBlobServiceClient(): BlobServiceClient {
    LOGGER.debug("Initializing access to azure storage blob service ...")

    val blobServiceClient =
        BlobServiceClientBuilder().connectionString(connectionString).buildClient()

    if (LOGGER.isDebugEnabled) {
      LOGGER.debug(
          "Blob storage has {} containers",
          blobServiceClient.listBlobContainers().spliterator().exactSizeIfKnown)
    }

    LOGGER.info(
        "Successfully initialized access to azure storage blob service {}",
        blobServiceClient.accountUrl)

    // Enable CORS for local azure storage account emulator
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      blobServiceClient.setLocalCors()
    }

    return blobServiceClient
  }

  @Bean
  @Primary
  open fun azureBlobContainerClient(
      blobServiceClient: BlobServiceClient,
      blobStorageProperties: BlobStorageProperties
  ): BlobContainerClient {
    val blobContainerClient =
        blobServiceClient.getBlobContainerClient(blobStorageProperties.containerName)

    if (!blobContainerClient.exists()) {
      val response = blobContainerClient.createWithResponse(null, null, null, Context.NONE)
      if (response.statusCode == CREATED.value()) {
        LOGGER.info("Container {} created", blobStorageProperties.containerName)
      } else if (response.statusCode >= BAD_REQUEST.value()) {
        LOGGER.error(
            "Container {} could not be created. Return code {} received.",
            blobStorageProperties.containerName,
            response.statusCode)
      } else {
        LOGGER.warn(
            "Unexpected return code {} received while trying to create container {}.",
            response.statusCode,
            blobStorageProperties.containerName)
      }
    }
    return blobContainerClient
  }

  companion object {
    private val LOGGER = getLogger(AzureBlobStorageAutoConfiguration::class.java)
  }
}
