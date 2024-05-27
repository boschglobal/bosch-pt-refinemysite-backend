/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.tracing

import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory

class DropUnwantedEventTableMetricsTracesInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    if (shouldDropWholeTrace(trace)) {
      // drop whole trace, including all spans
      LOGGER.debug("Dropping trace with {} spans", trace.size)
      return emptyList()
    }
    return trace
  }

  private fun shouldDropWholeTrace(trace: Collection<MutableSpan>) =
      trace.any { shouldDropWholeTrace(it) }

  private fun shouldDropWholeTrace(span: MutableSpan) =
      span.resourceName.toString() == "EventTableMetrics.updateMetrics"

  companion object {
    private val LOGGER =
        LoggerFactory.getLogger(DropUnwantedEventTableMetricsTracesInterceptor::class.java)
  }
}
