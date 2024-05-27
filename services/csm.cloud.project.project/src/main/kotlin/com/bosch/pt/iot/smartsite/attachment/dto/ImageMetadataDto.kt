/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2017
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.dto

import java.util.Date
import org.apache.commons.lang3.builder.ToStringBuilder

class ImageMetadataDto(
    var fileSize: Long = 0,
    var imageWidth: Long = 0,
    var imageHeight: Long = 0,
    var imageCreationDate: Date? = null
) {

  override fun toString(): String =
      ToStringBuilder(this)
          .append("fileSize", fileSize)
          .append("imageWidth", imageWidth)
          .append("imageHeight", imageHeight)
          .append("imageCreationDate", imageCreationDate)
          .toString()
}
