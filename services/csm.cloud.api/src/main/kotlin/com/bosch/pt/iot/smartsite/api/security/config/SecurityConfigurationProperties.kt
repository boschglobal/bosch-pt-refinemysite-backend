/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(IdentityProviderConfigurationProperties::class)
class SecurityProperties

@ConfigurationProperties(prefix = "custom.auth.idp")
data class IdentityProviderConfigurationProperties(
    val clientRegistration: String,
    val styleId: String,
    val styleIdParameter: String,
    val logoutUrl: URI,
)

@ConfigurationProperties(prefix = "custom.auth.myidp1")
data class MyIdp1ConfigurationProperties(
    val userProfileUrl: URI,
)
