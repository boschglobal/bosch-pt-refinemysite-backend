/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.config.QuarantineBlobStorageProperties
import com.bosch.pt.iot.smartsite.project.download.repository.DownloadBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.Configuration

@Configuration
open class BlobTestConfiguration {

  // Need to mock because we don't want to establish a real connection to the blob store
  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var blobStorageProperties: BlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var downloadBlobStorageProperties: DownloadBlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var downloadBlobStorageRepository: DownloadBlobStorageRepository

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var importBlobStorageProperties: ImportBlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var importBlobStorageRepository: ImportBlobStorageRepository

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var quarantineBlobStorageProperties: QuarantineBlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var quarantineBlobStorageRepository: QuarantineBlobStorageRepository
}
