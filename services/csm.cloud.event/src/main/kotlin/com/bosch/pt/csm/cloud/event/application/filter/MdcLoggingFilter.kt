/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.filter

import com.bosch.pt.csm.cloud.event.application.logging.MdcExtractor
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
@Order(1)
class MdcLoggingFilter : WebFilter {

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      chain
          .filter(exchange)
          .contextWrite { ctx: Context -> MdcExtractor.addFromRequest(ctx, exchange.request) }
          .then(Mono.empty())
}
