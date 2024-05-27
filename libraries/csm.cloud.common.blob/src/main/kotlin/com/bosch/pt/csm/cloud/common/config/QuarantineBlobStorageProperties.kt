/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.config

import com.bosch.pt.csm.cloud.common.config.BlobStorageProperties.Companion.DEFAULT_SHARED_ACCESS_EXPIRY_TIME
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.quarantine-storage")
open class QuarantineBlobStorageProperties {
  var containerName: String = DEFAULT_CONTAINER_NAME
  var sharedAccessExpiryTime: Int = DEFAULT_SHARED_ACCESS_EXPIRY_TIME
  lateinit var directory: String

  companion object {
    private const val DEFAULT_CONTAINER_NAME = "uploads"
  }
}
