/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.tracing

import com.bosch.pt.csm.cloud.common.tracing.AbstractTraceInterceptor
import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory.getLogger

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
    return (shouldDropNettyClientRequest(span) ||
        resourceName == "AzureStorageQueueMetrics.updateMetrics" ||
        resourceName == "ImageScalingListener.poll")
  }

  private fun shouldDropNettyClientRequest(span: MutableSpan) =
      isNettyClientRequest(span) && span.tags["http.url"].toString().contains("csm-imagescaling")

  private fun isNettyClientRequest(span: MutableSpan) =
      span.operationName.toString() == "netty.client.request"

  companion object {
    private val LOGGER = getLogger(DropUnwantedSpansInterceptor::class.java)
  }
}
