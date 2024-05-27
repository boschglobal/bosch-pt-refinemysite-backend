/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.config

import ch.qos.logback.classic.ClassicConstants
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.lang.NonNull
import org.springframework.web.filter.OncePerRequestFilter

class MdcInsertingRequestParametersFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
      @NonNull request: HttpServletRequest,
      @NonNull response: HttpServletResponse,
      @NonNull filterChain: FilterChain
  ) {
    try {
      insertIntoMdc(request)
      filterChain.doFilter(request, response)
    } finally {
      clearMdc()
    }
  }

  private fun insertIntoMdc(request: HttpServletRequest) {
    MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, request.remoteHost)
    MDC.put(ClassicConstants.REQUEST_REQUEST_URI, request.requestURI)
    val requestUrl = request.requestURL
    if (requestUrl != null) {
      MDC.put(ClassicConstants.REQUEST_REQUEST_URL, requestUrl.toString())
    }
    MDC.put(ClassicConstants.REQUEST_METHOD, request.method)
    MDC.put(ClassicConstants.REQUEST_QUERY_STRING, request.queryString)
    MDC.put(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY, request.getHeader("User-Agent"))
    MDC.put(ClassicConstants.REQUEST_X_FORWARDED_FOR, request.getHeader("X-Forwarded-For"))
  }

  private fun clearMdc() {
    MDC.remove(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY)
    MDC.remove(ClassicConstants.REQUEST_REQUEST_URI)
    MDC.remove(ClassicConstants.REQUEST_QUERY_STRING)
    MDC.remove(ClassicConstants.REQUEST_REQUEST_URL)
    MDC.remove(ClassicConstants.REQUEST_METHOD)
    MDC.remove(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY)
    MDC.remove(ClassicConstants.REQUEST_X_FORWARDED_FOR)
  }
}
