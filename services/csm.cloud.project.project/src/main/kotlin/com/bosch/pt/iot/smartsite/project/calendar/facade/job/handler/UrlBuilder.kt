/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.facade.job.handler

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder.fromHttpUrl

@Component
class UrlBuilder(
    private val apiVersionProperties: ApiVersionProperties,
    @Value("\${domain.name.api:localhost}") private val domainName: String
) {

  fun withPath(path: String): UriComponentsBuilder = fromHttpUrl("${getBaseUrl()}$path")

  private fun getBaseUrl() =
      "${getScheme()}$domainName${getPortIfLocal()}/internal/v${apiVersionProperties.version.max}"

  private fun getPortIfLocal() = if (domainName == "localhost") ":8090" else ""

  private fun getScheme() = if (domainName == "localhost") "http://" else "https://"
}
