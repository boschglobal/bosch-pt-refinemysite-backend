/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.api")
data class ApiVersionProperties(
    // version information of the api implemented by the service that uses this library
    val version: CurrentServiceVersion,
    // referenced version of the user api (if used)
    val user: ReferencedServiceVersion? = null
) {
  data class CurrentServiceVersion(val min: Int, val max: Int, val prefix: String)
  data class ReferencedServiceVersion(val version: Int)
}
