/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.docs

import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/** Block accesses to the index.html if docs are not provided in the environment */
@Component
@Profile("!docs")
class BlockDocsFilter : WebFilter {

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      if (exchange.request.uri.path == "/internal/docs/index.html") {
        Mono.error(ResponseStatusException(NOT_FOUND))
      } else chain.filter(exchange)
}
