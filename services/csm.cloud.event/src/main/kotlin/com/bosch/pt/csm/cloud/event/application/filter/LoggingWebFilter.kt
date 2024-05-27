package com.bosch.pt.csm.cloud.event.application.filter

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Profile("log-requests")
@Component
class LoggingWebFilter : WebFilter {

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
    LOGGER.info("Request {} called", exchange.request.path.value())
    return chain.filter(exchange)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(LoggingWebFilter::class.java)
  }
}
