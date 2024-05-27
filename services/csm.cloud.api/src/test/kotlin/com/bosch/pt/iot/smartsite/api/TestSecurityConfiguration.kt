/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers

/** Configuration which allows all access. Used to deactivate our configuration for tests */
@EnableWebFluxSecurity
@Profile("test-without-security")
class TestSecurityConfiguration {
  @Bean
  @Primary
  fun allowAllSecurity(
      http: ServerHttpSecurity,
  ): SecurityWebFilterChain =
      http
          .securityMatcher(pathMatchers(""))
          .authorizeExchange { authorize -> authorize.anyExchange().permitAll() }
          .build()
}
