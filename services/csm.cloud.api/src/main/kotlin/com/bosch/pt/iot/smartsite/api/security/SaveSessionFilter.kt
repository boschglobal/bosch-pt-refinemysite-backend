/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security

import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class SaveSessionFilter : WebFilter {
  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> =
      exchange.session.flatMap { it.save() }.then(chain.filter(exchange))
}