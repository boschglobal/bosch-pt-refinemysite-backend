/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.config

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.blob-storage")
open class BlobStorageProperties {

  var containerName: String = DEFAULT_CONTAINER_NAME
  var sharedAccessExpiryTime: Int = DEFAULT_SHARED_ACCESS_EXPIRY_TIME
  var image: Image = Image()

  open fun getImageFolder(imageResolution: ImageResolution): String {
    return when (imageResolution) {
      ImageResolution.ORIGINAL -> image.folder.original
      ImageResolution.SMALL -> image.folder.small
      ImageResolution.MEDIUM -> image.folder.medium
      ImageResolution.FULL -> image.folder.fullhd
    }
  }

  open class Image {
    val folder: Folder = Folder()
  }

  open class Folder {
    var original: String = ORIGINAL
    var fullhd: String = FULL_HD
    var medium: String = MEDIUM
    var small: String = SMALL
  }

  companion object {
    const val DEFAULT_SHARED_ACCESS_EXPIRY_TIME = 60
    private const val DEFAULT_CONTAINER_NAME = "csm"
    private const val ORIGINAL = "image/original"
    private const val FULL_HD = "image/fullhd"
    private const val MEDIUM = "image/medium"
    private const val SMALL = "image/small"
  }
}
