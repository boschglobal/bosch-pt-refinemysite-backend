/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import org.springframework.cloud.gateway.config.GlobalCorsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfiguration {

  @Bean
  @Order(HIGHEST_PRECEDENCE)
  fun corsFilter(globalCorsProperties: GlobalCorsProperties): CorsWebFilter =
      CorsWebFilter(
          UrlBasedCorsConfigurationSource().apply {
            setCorsConfigurations(globalCorsProperties.corsConfigurations)
          })
}
