/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

data class BlobProperties(val blobSize: Long)

enum class MalwareScanResult(val text: String) {
  NOT_SCANNED(""),
  SAFE("No threats found"),
  NOT_SAFE("Malicious");

  companion object {
    fun fromText(text: String?): MalwareScanResult =
        MalwareScanResult.values().firstOrNull { it.text == text } ?: NOT_SCANNED
  }
}
