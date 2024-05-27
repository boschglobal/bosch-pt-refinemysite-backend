/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties
import com.bosch.pt.csm.cloud.projectmanagement.attachment.service.BlobStoreService
import io.mockk.mockk
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class BlobTestConfiguration {

  // Need to mock because we don't want to establish a real connection to the blob store
  @Suppress("UnusedPrivateMember")
  @MockBean
  private val blobStorageProperties: BlobStorageProperties? = null

  @Suppress("UnusedPrivateMember")
  @MockBean
  private val azureBlobStorageRepository: AzureBlobStorageRepository? = null

  @Bean @Primary fun overrideBlobStoreService(): BlobStoreService = mockk()
}
