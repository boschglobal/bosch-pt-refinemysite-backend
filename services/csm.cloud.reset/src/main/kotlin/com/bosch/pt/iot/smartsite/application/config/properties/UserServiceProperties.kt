/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(UserServiceProperties::class)
class UserServicePropertiesConfiguration

@ConfigurationProperties("service.user")
class UserServiceProperties(
    val insertStatement: String,
    val insertMessageStatement: String,
    val topics: List<String> = emptyList(),
    val blobContainers: List<String> = emptyList(),
    val azureQueues: List<String> = emptyList()
)
