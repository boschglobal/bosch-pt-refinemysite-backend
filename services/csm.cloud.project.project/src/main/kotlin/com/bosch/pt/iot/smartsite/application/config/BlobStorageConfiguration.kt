/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.config.setLocalCors
import com.azure.core.util.Context
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpStatus

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractBlobStorageConfiguration(
    val properties: AbstractBlobStorageProperties,
    val environment: Environment,
    val logger: Logger,
    val name: String
) {

  open fun blobStorageServiceClient(environment: Environment): BlobServiceClient {
    logger.debug("Initializing access to azure storage blob service for $name ...")
    val blobServiceClient =
        BlobServiceClientBuilder().connectionString(properties.connectionString).buildClient()

    if (logger.isDebugEnabled) {
      logger.debug(
          "Blob storage for $name has " +
              "${blobServiceClient.listBlobContainers().spliterator().exactSizeIfKnown} containers")
    }

    logger.info(
        "Successfully initialized access to azure storage blob service for $name ${blobServiceClient.accountUrl}",
    )

    // Enable CORS for local azure storage account emulator
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      blobServiceClient.setLocalCors()
    }

    return blobServiceClient
  }

  open fun blobContainerClient(
      blobServiceClient: BlobServiceClient,
      properties: AbstractBlobStorageProperties
  ): BlobContainerClient {
    val blobContainerClient = blobServiceClient.getBlobContainerClient(properties.containerName)
    if (!blobContainerClient.exists()) {
      val response = blobContainerClient.createWithResponse(null, null, null, Context.NONE)
      if (response.statusCode == HttpStatus.CREATED.value()) {
        logger.info("Container ${properties.containerName} created")
      } else if (response.statusCode >= 400) {
        logger.error(
            "Container ${properties.containerName} could not be created. " +
                "Return code ${response.statusCode} received.")
      } else {
        logger.warn(
            "Unexpected return code ${response.statusCode} received " +
                "while trying to create container ${properties.containerName}.")
      }
    }
    return blobContainerClient
  }
}

@Profile("!test")
@Configuration
@EnableConfigurationProperties(DownloadBlobStorageProperties::class)
open class DownloadBlobStorageConfiguration(
    properties: DownloadBlobStorageProperties,
    environment: Environment,
    logger: Logger
) : AbstractBlobStorageConfiguration(properties, environment, logger, "downloads") {

  /**
   * This service client is for the storage account that stores the calendar exports to be
   * downloaded at the end of a calendar export job. We set up a dedicated storage account since the
   * downloads get auto-removed after a defined timespan using a lifecycle rule of the storage
   * account. To not accidentally delete other blobs by having a wrong prefix in this rule, we
   * decided to go for a dedicated storage account.
   */
  @Bean
  @Qualifier("downloadServiceClient")
  open fun downloadsBlobStorageServiceClient(environment: Environment): BlobServiceClient =
      super.blobStorageServiceClient(environment)

  @Bean
  @Qualifier("downloadContainerClient")
  open fun downloadsBlobContainerClient(
      @Qualifier("downloadServiceClient") blobServiceClient: BlobServiceClient,
      properties: DownloadBlobStorageProperties
  ): BlobContainerClient = super.blobContainerClient(blobServiceClient, properties)
}

@Profile("!test")
@Configuration
@EnableConfigurationProperties(ImportBlobStorageProperties::class)
open class ImportBlobStorageConfiguration(
    properties: ImportBlobStorageProperties,
    environment: Environment,
    logger: Logger
) : AbstractBlobStorageConfiguration(properties, environment, logger, "imports") {

  /**
   * This service client is for the storage account that stores the uploaded files for the import.
   */
  @Bean
  @Qualifier("importServiceClient")
  open fun importsBlobStorageServiceClient(environment: Environment): BlobServiceClient =
      super.blobStorageServiceClient(environment)

  @Bean
  @Qualifier("importContainerClient")
  open fun importsBlobContainerClient(
      @Qualifier("importServiceClient") blobServiceClient: BlobServiceClient,
      properties: ImportBlobStorageProperties
  ): BlobContainerClient = super.blobContainerClient(blobServiceClient, properties)
}

@Profile("!test")
@Configuration
@EnableConfigurationProperties(QuarantineBlobStorageProperties::class)
open class ImportQuarantineBlobStorageConfiguration(
    properties: QuarantineBlobStorageProperties,
    environment: Environment,
    logger: Logger
) : AbstractBlobStorageConfiguration(properties, environment, logger, "quarantine") {
  /** This service client is for the quarantine storage account to upload imports to */
  @Bean
  @Qualifier("importQuarantineServiceClient")
  open fun importsQuarantineBlobStorageServiceClient(environment: Environment): BlobServiceClient =
      super.blobStorageServiceClient(environment)

  @Bean
  @Qualifier("importQuarantineContainerClient")
  open fun importsQuarantineBlobContainerClient(
      @Qualifier("importQuarantineServiceClient") blobServiceClient: BlobServiceClient,
      properties: QuarantineBlobStorageProperties
  ): BlobContainerClient = super.blobContainerClient(blobServiceClient, properties)
}
