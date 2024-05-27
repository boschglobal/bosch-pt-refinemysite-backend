/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.config

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@AutoConfiguration
@ConditionalOnProperty("custom.quarantine-storage.connection-string")
@EnableConfigurationProperties(QuarantineBlobStorageProperties::class)
@Import(AzureBlobStorageAutoConfiguration::class)
open class QuarantineBlobStorageAutoConfiguration(
    @Value("\${custom.quarantine-storage.connection-string}") private val connectionString: String,
    private val environment: Environment
) {

  @Bean
  open fun quarantineBlobStorageRepository(
      quarantineBlobStorageProperties: QuarantineBlobStorageProperties,
      @Qualifier("quarantineBlobContainerClient") quarantineClient: BlobContainerClient?,
  ): QuarantineBlobStorageRepository =
      QuarantineBlobStorageRepository(quarantineBlobStorageProperties, quarantineClient)

  @Bean
  open fun quarantineBlobServiceClient(): BlobServiceClient {
    LOGGER.debug("Initializing access to quarantine storage...")

    val blobServiceClient =
        BlobServiceClientBuilder().connectionString(connectionString).buildClient()

    // Enable CORS for local azure storage account emulator
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      blobServiceClient.setLocalCors()
    }

    return blobServiceClient
  }

  @Bean
  open fun quarantineBlobContainerClient(
      @Qualifier("quarantineBlobServiceClient") blobServiceClient: BlobServiceClient,
      quarantineBlobStorageProperties: QuarantineBlobStorageProperties
  ): BlobContainerClient =
      blobServiceClient.getBlobContainerClient(quarantineBlobStorageProperties.containerName).let {
        it.createIfNotExists() // needed so locally the container is created in azurite
        return it
      }

  companion object {
    private val LOGGER = getLogger(QuarantineBlobStorageAutoConfiguration::class.java)
  }
}
