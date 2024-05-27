/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.azure.storage.blob.BlobServiceClient
import com.bosch.pt.iot.smartsite.application.config.properties.UserServiceProperties
import com.bosch.pt.iot.smartsite.reset.Resettable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class UserServiceBlobStorageResetService(
    @Qualifier("blobServiceClientForUserServiceImages") blobServiceClient: BlobServiceClient,
    private val userServiceProperties: UserServiceProperties
) : AbstractBlobStorageResetService(blobServiceClient), Resettable {

  override fun reset() {
    LOGGER.info("Resetting blob containers (from azure storage account) for user service ...")
    userServiceProperties
        .blobContainers
        .mapNotNull { getContainerReference(it) }
        .filter { exists(it) }
        .forEach { deleteAllBlobs(it) }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserServiceBlobStorageResetService::class.java)
  }
}
