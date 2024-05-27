package com.bosch.pt.csm.cloud.event.application.filter

import com.bosch.pt.csm.cloud.event.application.config.ReactiveRequestContextHolder.CONTEXT_KEY
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
class ReactiveRequestContextFilter : WebFilter {

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      chain.filter(exchange).contextWrite { ctx: Context -> ctx.put(CONTEXT_KEY, exchange.request) }
}
