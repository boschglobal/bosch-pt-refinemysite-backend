/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.logging

import ch.qos.logback.classic.ClassicConstants.REQUEST_METHOD
import ch.qos.logback.classic.ClassicConstants.REQUEST_QUERY_STRING
import ch.qos.logback.classic.ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY
import ch.qos.logback.classic.ClassicConstants.REQUEST_REQUEST_URI
import ch.qos.logback.classic.ClassicConstants.REQUEST_USER_AGENT_MDC_KEY
import ch.qos.logback.classic.ClassicConstants.REQUEST_X_FORWARDED_FOR
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.util.context.Context

object MdcExtractor {

  fun addFromRequest(context: Context, request: ServerHttpRequest): Context =
      request
          .apply {
            add(context, REQUEST_METHOD, this.method.name())
            add(context, REQUEST_REMOTE_HOST_MDC_KEY, this.remoteAddress?.hostString)
            add(context, REQUEST_REQUEST_URI, this.path.toString())
            add(context, REQUEST_USER_AGENT_MDC_KEY, this.headers.getFirst(USER_AGENT))
            add(context, REQUEST_X_FORWARDED_FOR, this.headers.getFirst("X-Forwarded-For"))
            add(
                context,
                REQUEST_QUERY_STRING,
                this.queryParams.entries.map { "${it.key}=${it.value}" }.reduceOrNull { p1, p2 ->
                  "$p1&$p2"
                })
          }
          .let { context }

  private fun add(context: Context, key: Any, value: Any?) = value?.let { context.put(key, it) }
}
