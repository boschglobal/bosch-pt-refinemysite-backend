/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing.request.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.request.filter")
data class RequestFilterProperties(
    val default: RequestSizeFilterProperties,
    val large: RequestSizeFilterProperties
)

data class RequestSizeFilterProperties(
    val paths: List<String>?,
    val maxSizeInMb: Long,
)
