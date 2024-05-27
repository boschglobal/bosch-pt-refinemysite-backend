/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing.request.filter

import com.bosch.pt.iot.smartsite.api.routing.request.matcher.LargeRequestServerWebExchangeMatcher
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Distinguishes between large and default request size limits based on
 * LargeRequestServerWebExchangeMatcher. Global filters are applied to all routes.
 */
class GlobalRequestSizeFilter(
    private val largeRequestSizeFilter: GatewayFilter,
    private val defaultRequestSizeFilter: GatewayFilter,
    private val largeRequestServerWebExchangeMatcher: LargeRequestServerWebExchangeMatcher,
) : GlobalFilter {

  override fun filter(exchange: ServerWebExchange?, chain: GatewayFilterChain?): Mono<Void>? {

    return largeRequestServerWebExchangeMatcher
        .matches(exchange)
        .map {
          if (it.isMatch) {
            largeRequestSizeFilter
          } else {
            defaultRequestSizeFilter
          }
        }
        .flatMap { it.filter(exchange, chain) }
  }
}
