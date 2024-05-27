/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.dto

import com.bosch.pt.csm.cloud.usermanagement.attachment.util.dto.ImageMetadataDto
import java.util.Date
import org.apache.commons.lang3.RandomUtils

class ImageMetadataDtoBuilder private constructor() {

  private var fileSize = 0
  private var imageWidth: Long = 0
  private var imageHeight: Long = 0
  private var imageCreationDate: Date? = null

  fun withFileSize(fileSize: Int): ImageMetadataDtoBuilder {
    this.fileSize = fileSize
    return this
  }

  fun withImageWidth(imageWidth: Long): ImageMetadataDtoBuilder {
    this.imageWidth = imageWidth
    return this
  }

  fun withImageHeight(imageHeight: Long): ImageMetadataDtoBuilder {
    this.imageHeight = imageHeight
    return this
  }

  fun withImageCreationDate(imageCreationDate: Date?): ImageMetadataDtoBuilder {
    this.imageCreationDate = imageCreationDate
    return this
  }

  fun build() = ImageMetadataDto(fileSize.toLong(), imageWidth, imageHeight, imageCreationDate)

  companion object {

    fun imageMetadataDto() =
        ImageMetadataDtoBuilder()
            .withFileSize(RandomUtils.nextInt())
            .withImageHeight(RandomUtils.nextLong())
            .withImageWidth(RandomUtils.nextLong())
            .withImageCreationDate(Date())
  }
}
