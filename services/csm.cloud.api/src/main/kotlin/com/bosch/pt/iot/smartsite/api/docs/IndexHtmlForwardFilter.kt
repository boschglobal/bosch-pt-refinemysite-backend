/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.docs

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * This filter forwards the base path "/" to index.html which contains the root page of the api
 * documentation.
 */
@Component
@Profile("docs")
class IndexHtmlForwardFilter : WebFilter {

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      when (exchange.request.uri.path) {
        in setOf("/internal", "/internal/", "/internal/docs", "/internal/docs/") ->
            chain.filter(
                exchange
                    .mutate()
                    .request(exchange.request.mutate().path("/internal/docs/index.html").build())
                    .build())
        else -> chain.filter(exchange)
      }
}
