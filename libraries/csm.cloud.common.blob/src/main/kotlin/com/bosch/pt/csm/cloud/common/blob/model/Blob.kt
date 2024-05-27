/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

class Blob(
    val blobName: String,
    val data: ByteArray,
    val metadata: BlobMetadata,
    val mimeType: String
)
