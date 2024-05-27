/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.azure.storage.blob.BlobServiceClient
import com.bosch.pt.iot.smartsite.application.config.properties.ProjectServiceProperties
import com.bosch.pt.iot.smartsite.reset.Resettable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProjectServiceBlobStorageResetService(
    @Qualifier("blobServiceClientForProjectServiceImages") blobServiceClient: BlobServiceClient,
    private val projectServiceProperties: ProjectServiceProperties
) : AbstractBlobStorageResetService(blobServiceClient), Resettable {

  override fun reset() {
    LOGGER.info("Resetting blob containers (from azure storage account) for project service ...")
    projectServiceProperties
        .blobContainers
        .mapNotNull { containerName: String -> getContainerReference(containerName) }
        .filter { exists(it) }
        .forEach { deleteAllBlobs(it) }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProjectServiceBlobStorageResetService::class.java)
  }
}
