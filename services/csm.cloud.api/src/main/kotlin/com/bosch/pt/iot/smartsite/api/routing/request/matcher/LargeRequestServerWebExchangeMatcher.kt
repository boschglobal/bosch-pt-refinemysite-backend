/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.routing.request.matcher

import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.matchers
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono

/** Matches if at least one of the versioned paths matches a server web exchange. */
class LargeRequestServerWebExchangeMatcher(paths: List<String>) : ServerWebExchangeMatcher {

  @Suppress("SpreadOperator")
  private val matcher: ServerWebExchangeMatcher =
      matchers(*paths.map { it.toVersionedPathMatcher() }.toTypedArray())

  override fun matches(exchange: ServerWebExchange?): Mono<ServerWebExchangeMatcher.MatchResult> =
      matcher.matches(exchange)

  private fun String.toVersionedPathMatcher(): PathPatternParserServerWebExchangeMatcher {
    return PathPatternParserServerWebExchangeMatcher(
        PathPatternParser.defaultInstance.parse(versionedPath(this)))
  }

  private fun versionedPath(path: String): String = "/{version:v[1-9][0-9]*}$path"
}
