/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.attachment.model

import java.util.Date

data class ImageMetadata(
    val imageWidth: Long? = null,
    val imageHeight: Long? = null,
    val imageType: ImageType? = null,
    val imageCreationDate: Date? = null
)
