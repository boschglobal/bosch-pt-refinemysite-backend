/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.model

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata

class BlobBuilder private constructor() {

  private var blobName: String? = null
  private var metadata: BlobMetadata? = null
  private var data: ByteArray? = null
  private var mimeType: String? = null

  fun withBlobName(blobName: String?): BlobBuilder = apply { this.blobName = blobName }

  fun withMetadata(metadata: BlobMetadata?): BlobBuilder = apply { this.metadata = metadata }

  fun withData(data: ByteArray): BlobBuilder = apply { this.data = data }

  fun withMimeType(mimeType: String?): BlobBuilder = apply { this.mimeType = mimeType }

  fun build(): Blob = Blob(blobName!!, data!!, metadata!!, mimeType!!)

  companion object {

    @JvmStatic
    fun blob(): BlobBuilder =
        BlobBuilder()
            .withBlobName("myBlob")
            .withData(ByteArray(0))
            .withMetadata(BlobMetadata.fromMap(emptyMap()))
            .withMimeType("image/jpeg")
  }
}
