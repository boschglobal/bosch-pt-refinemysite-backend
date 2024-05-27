/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.blob.repository.QuarantineBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.common.config.QuarantineBlobStorageProperties
import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.Configuration

@Configuration
class BlobTestConfiguration {

  // Need to mock because we don't want to establish a real connection to the blob store
  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var blobStorageProperties: BlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var azureBlobStorageRepository: AzureBlobStorageRepository

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var quarantineBlobStorageProperties: QuarantineBlobStorageProperties

  @Suppress("UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var quarantineBlobStorageRepository: QuarantineBlobStorageRepository
}
