/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.config

import com.azure.core.util.Context
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.BlobCorsRule
import com.azure.storage.blob.models.BlobServiceProperties
import com.bosch.pt.csm.cloud.common.config.CorsConstants.AGE_MAX_SECONDS
import com.bosch.pt.csm.cloud.common.config.CorsConstants.REQUEST_TIME_OUT
import java.time.Duration

fun BlobServiceClient.setLocalCors() {

  val corsRule = BlobCorsRule()
  corsRule.allowedHeaders = "*"
  corsRule.allowedOrigins =
      "http://localhost:8000,http://localhost:8001,null,http://127.0.0.1:8000,http://127.0.0.1:8001"
  corsRule.allowedMethods = "GET, OPTIONS, POST, PUT"
  corsRule.exposedHeaders = "*"
  corsRule.maxAgeInSeconds = AGE_MAX_SECONDS

  val properties = BlobServiceProperties()
  properties.cors = listOf(corsRule)
  this.setPropertiesWithResponse(properties, Duration.ofSeconds(REQUEST_TIME_OUT), Context.NONE)
}

private object CorsConstants {
  const val AGE_MAX_SECONDS = 86400
  const val REQUEST_TIME_OUT = 10L
}
