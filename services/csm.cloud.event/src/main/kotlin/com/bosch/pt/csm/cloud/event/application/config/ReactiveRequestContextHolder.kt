package com.bosch.pt.csm.cloud.event.application.config

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.util.context.ContextView

object ReactiveRequestContextHolder {

  val CONTEXT_KEY = ServerWebExchange::class.java

  fun getRequest(): Mono<ServerWebExchange> =
      Mono.deferContextual { ctx: ContextView -> Mono.just(ctx.get(CONTEXT_KEY)) }
}
