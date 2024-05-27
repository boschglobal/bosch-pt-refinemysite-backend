/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.attachment.util.dto

import java.util.Date

data class ImageMetadataDto(
    val fileSize: Long = 0,
    val imageWidth: Long = 0,
    val imageHeight: Long = 0,
    val imageCreationDate: Date? = null
)
