/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.mobileversion

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MobileVersionProperty::class)
class MobileVersionPropertyConfiguration

@ConfigurationProperties(prefix = "custom.mobile")
data class MobileVersionProperty(val lastSupportedVersion: String = "0.0.0")
