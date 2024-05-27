/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import com.bosch.pt.iot.smartsite.api.security.config.FallbackTokenRelayGatewayFilterConfiguration.FallbackTokenRelayGatewayFilterFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory.NameConfig
import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.server.ServerWebExchange

/** Configures a [FallbackTokenRelayGatewayFilterFactory]. */
@Configuration
class FallbackTokenRelayGatewayFilterConfiguration {

  @Bean
  fun fallbackTokenRelayGatewayFilterFactory(
      tokenRelayGatewayFilterFactory: TokenRelayGatewayFilterFactory
  ) = FallbackTokenRelayGatewayFilterFactory(tokenRelayGatewayFilterFactory)

  /**
   * A GatewayFilterFactory that delegates to [TokenRelayGatewayFilterFactory] as a fallback only if
   * there is no pre-existing _Authorization_ header set already on the request. This prioritizes an
   * _Authorization_ header (assumed to be deliberately set) over a session-stored OAuth-based
   * Access Token that is actually retrieved from the session triggered by the
   * [TokenRelayGatewayFilterFactory]-issued filter.
   */
  class FallbackTokenRelayGatewayFilterFactory(
      private val tokenRelayGatewayFilterFactory: TokenRelayGatewayFilterFactory
  ) : AbstractGatewayFilterFactory<NameConfig>(NameConfig::class.java) {

    fun apply(): GatewayFilter = this.apply(null as NameConfig?)

    override fun apply(config: NameConfig?): GatewayFilter {
      val tokenRelayGatewayFilter = tokenRelayGatewayFilterFactory.apply(config)
      return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        if (exchange.request.headers.containsKey(AUTHORIZATION)) {
          chain.filter(exchange)
        } else {
          tokenRelayGatewayFilter.filter(exchange, chain)
        }
      }
    }
  }
}
