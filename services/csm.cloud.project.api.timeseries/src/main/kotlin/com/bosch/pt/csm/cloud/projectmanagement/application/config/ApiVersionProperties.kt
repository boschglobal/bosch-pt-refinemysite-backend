/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ApiVersionProperties::class)
class ApiVersionPropertiesConfiguration

@ConfigurationProperties(prefix = "custom.api")
data class ApiVersionProperties(
    val authenticationStatus: Version = Version(),
    val company: Version = Version(),
    val project: Version = Version(),
    val translation: Version = Version(),
    val user: Version = Version(),
    val unknown: Version = Version()
) {

  data class Version(
      val min: Int = 1,
      val max: Int = 1,
  )
}
