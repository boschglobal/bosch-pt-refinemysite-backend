/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Holds the list of trusted issuer-uri configured under
 * 'custom.security.oauth2.resource-server.jwt.issuer-uris'. Will be enabled by
 * [CustomWebSecurityAutoConfiguration].
 */
@ConfigurationProperties("custom.security.oauth2.resource-server.jwt")
data class CustomTrustedJwtIssuersProperties(
    val issuerUris: List<String> = emptyList(),
)
