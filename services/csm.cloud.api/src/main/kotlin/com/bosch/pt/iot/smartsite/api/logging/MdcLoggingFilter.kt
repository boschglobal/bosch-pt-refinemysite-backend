/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.api.logging

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
@Order(1)
class MdcLoggingFilter : GlobalFilter {

  override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> =
      chain
          .filter(exchange)
          .contextWrite { ctx: Context -> MdcExtractor.addFromRequest(ctx, exchange.request) }
          .then(Mono.empty())
}
