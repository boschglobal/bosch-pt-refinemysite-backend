/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.tracing

import datadog.trace.api.interceptor.MutableSpan
import datadog.trace.api.interceptor.TraceInterceptor
import org.slf4j.LoggerFactory

abstract class AbstractTraceInterceptor(private val priority: Int) : TraceInterceptor {

  override fun onTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    try {
      return handleTraceComplete(trace)
    } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
      LOGGER.error("Caught exception in trace interceptor", ex)
      throw ex
    }
  }

  abstract fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan>

  override fun priority() = priority // must be unique across all trace interceptors!

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractTraceInterceptor::class.java)
  }
}
