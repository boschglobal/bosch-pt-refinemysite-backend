/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Holds the list of trusted issuer-uri configured under
 * 'custom.security.oauth2.resource-server.jwt.issuer-uris'.
 */
@LibraryCandidate("There is a common.web version which is currently not used in reactive services")
@ConfigurationProperties("custom.security.oauth2.resource-server.jwt")
data class CustomTrustedJwtIssuersProperties(
    val issuerUris: List<String> = emptyList(),
)
