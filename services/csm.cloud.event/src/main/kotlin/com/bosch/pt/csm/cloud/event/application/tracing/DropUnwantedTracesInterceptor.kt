/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.event.application.tracing

import com.bosch.pt.csm.cloud.common.tracing.AbstractTraceInterceptor
import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory

class DropUnwantedTracesInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    if (isHeartBeatGenerator(trace)) {
      // drop whole trace, including all spans
      LOGGER.debug("Dropping trace with {} spans", trace.size)
      return emptyList()
    }
    return trace
  }

  private fun isHeartBeatGenerator(trace: Collection<MutableSpan>) =
      trace.any { isHeartBeatGenerator(it) }

  private fun isHeartBeatGenerator(span: MutableSpan) =
      span.resourceName.toString() == "SpringIntegrationConfiguration.generateHeartBeat"

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DropUnwantedTracesInterceptor::class.java)
  }
}
