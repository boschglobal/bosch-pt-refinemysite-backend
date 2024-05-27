/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.FULL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.SMALL

/** An entity that owns an image [Blob]. */
interface ImageBlobOwner : BlobOwner {

  fun isSmallAvailable(): Boolean

  fun setSmallAvailable(available: Boolean)

  fun isFullAvailable(): Boolean

  fun setFullAvailable(available: Boolean)

  fun isResolutionAvailable(imageResolution: ImageResolution): Boolean {
    return when (imageResolution) {
      SMALL -> isSmallAvailable()
      FULL -> isFullAvailable()
      // original resolution is always available unless the Blob is deleted
      ORIGINAL -> true
      else -> throw IllegalArgumentException("Unknown image resolution: $imageResolution")
    }
  }

  fun setResolutionAvailable(imageResolution: ImageResolution) {
    when (imageResolution) {
      SMALL -> setSmallAvailable(true)
      FULL -> setFullAvailable(true)
      ORIGINAL -> {}
      else -> throw IllegalArgumentException("Unknown image resolution: $imageResolution")
    }
  }

  /**
   * Returns the given image resolution or falls back to returning the original resolution in case
   * the requested resolution is not available (yet).
   *
   * @param imageResolution the requested image resolution
   * @return the specified image resolution, if available; else [ImageResolution.ORIGINAL]
   */
  fun getResolutionOrOriginal(imageResolution: ImageResolution): ImageResolution {
    return if (isResolutionAvailable(imageResolution)) imageResolution else ORIGINAL
  }
}
