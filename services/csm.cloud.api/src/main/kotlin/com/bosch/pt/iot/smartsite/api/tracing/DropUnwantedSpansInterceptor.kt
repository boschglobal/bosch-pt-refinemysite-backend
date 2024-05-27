/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.api.tracing

import com.bosch.pt.csm.cloud.common.tracing.AbstractTraceInterceptor
import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory

class DropUnwantedSpansInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    val filteredTrace: MutableList<MutableSpan> = mutableListOf()
    for (span in trace) {
      if (!shouldDropSpan(span)) {
        filteredTrace.add(span)
      } else {
        if (LOGGER.isDebugEnabled) {
          LOGGER.debug("Dropping span: {}", span)
        }
      }
    }
    return filteredTrace
  }

  private fun shouldDropSpan(span: MutableSpan): Boolean {
    val resourceName = span.resourceName.toString()
    return resourceName == "FilteringWebHandler.handle"
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DropUnwantedSpansInterceptor::class.java)
  }
}
