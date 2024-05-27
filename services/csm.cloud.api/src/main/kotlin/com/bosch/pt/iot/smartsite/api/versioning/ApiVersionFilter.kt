/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.versioning

import java.util.regex.Pattern
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class ApiVersionFilter(private val properties: ApiVersionProperties) : GlobalFilter, Ordered {

  override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
    val request = exchange.request

    val requestPath = request.path.toString()
    if (requestPath.startsWith("/timeline")) {
      val version = request.getApiVersionOrFail()
      val api = request.getApiName() ?: ""
      assertTimelineApiVersionIsSupported(api, version)
    } else if (requestPath.startsWith("/graphql/v")) {
      val version = request.getApiVersionOrFail()
      assertGraphQlApiVersionIsSupported(version)
    } else if (requestPath.startsWith("/v") || requestPath.startsWith("/internal/v")) {
      val version = request.getApiVersionOrFail()
      val api = request.getApiName() ?: ""
      assertInternalApiVersionIsSupported(api, version)
    }

    return chain.filter(exchange)
  }

  private fun ServerHttpRequest.getApiVersionOrFail(): Int {
    val path = this.path.toString()
    val matcher = API_VERSION_PATTERN.matcher(path)
    if (matcher.find()) {
      return matcher.group("version").toInt()
    } else {
      error("Missing version in path $path")
    }
  }

  private fun ServerHttpRequest.getApiName(): String? {
    val path = this.path.toString()
    val matcher = API_NAME_PATTERN.matcher(path)
    if (matcher.find()) {
      return matcher.group("name")
    }
    return null
  }

  private fun assertTimelineApiVersionIsSupported(api: String, version: Int) =
      when (api) {
        "" -> properties.timeline.authenticationStatus
        "companies" -> properties.timeline.company
        "projects" -> properties.timeline.project
        "translations" -> properties.timeline.translation
        "users" -> properties.timeline.user
        else -> properties.internal.unknown
      }.assertVersionIsSupported(version)

  private fun assertGraphQlApiVersionIsSupported(version: Int) =
      properties.graphql.assertVersionIsSupported(version)

  private fun assertInternalApiVersionIsSupported(api: String, version: Int) =
      when (api) {
        "announcements" -> properties.internal.announcement
        "bim" -> properties.internal.bimModel
        "companies" -> properties.internal.company
        "crafts" -> properties.internal.craft
        "documents" -> properties.internal.documents
        "events" -> properties.internal.event
        "features" -> properties.internal.feature
        "jobs" -> properties.internal.job
        "projects" -> properties.internal.project
        "users" -> properties.internal.user
        else -> properties.internal.unknown
      }.assertVersionIsSupported(version)

  private fun Version.assertVersionIsSupported(version: Int) {
    if (version < this.min || version > this.max) {
      throw UnsupportedApiVersionException(this.min, this.max)
    }
  }

  companion object {
    private val API_VERSION_PATTERN: Pattern = Pattern.compile("/v(?<version>[0-9]+)/?")
    private val API_NAME_PATTERN: Pattern = Pattern.compile("/v[0-9]+/(?<name>[^/]*)/?")
  }

  // this filter needs to run before (!) the path rewriting filter (which has order 0) to be able to
  // capture the unaltered request path
  override fun getOrder(): Int = -1
}
