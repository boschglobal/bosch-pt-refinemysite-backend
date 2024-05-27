/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.errors

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@Order(6)
class BadGatewayHandler : GlobalFilter {

  override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> =
      chain.filter(exchange).doOnError { ex: Throwable? ->
        // Null pointer exception is ambiguous but means service could not be reached
        // in most cases
        if (ServerWebExchangeUtils.isAlreadyRouted(exchange)) {
          throw ResponseStatusException(HttpStatus.BAD_GATEWAY, null, ex)
        }
      }
}
