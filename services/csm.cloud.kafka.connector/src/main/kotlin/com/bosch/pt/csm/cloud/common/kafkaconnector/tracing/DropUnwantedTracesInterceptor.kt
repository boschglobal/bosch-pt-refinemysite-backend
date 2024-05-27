/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.tracing

import com.bosch.pt.csm.cloud.common.tracing.AbstractTraceInterceptor
import datadog.trace.api.interceptor.MutableSpan
import org.slf4j.LoggerFactory

class DropUnwantedTracesInterceptor(priority: Int) : AbstractTraceInterceptor(priority) {

  override fun handleTraceComplete(trace: Collection<MutableSpan>): Collection<MutableSpan> {
    if (isScheduleServiceRun(trace) && !hasSentKafkaEvents(trace)) {
      // drop whole trace, including all spans
      LOGGER.debug("Dropping trace with {} spans", trace.size)
      return emptyList()
    }
    return trace
  }

  private fun isScheduleServiceRun(trace: Collection<MutableSpan>) =
      trace.any { isScheduleServiceRun(it) }

  private fun isScheduleServiceRun(span: MutableSpan) =
      span.resourceName.toString() == "ScheduleService.run"

  private fun hasSentKafkaEvents(trace: Collection<MutableSpan>) =
      trace.any { hasSentKafkaEvents(it) }

  private fun hasSentKafkaEvents(span: MutableSpan) =
      span.resourceName.toString() == "KafkaFeedService.feedBatch" &&
          span.tags.containsKey("events.count") &&
          span.tags["events.count"] as Int > 0

  companion object {
    private val LOGGER = LoggerFactory.getLogger(DropUnwantedTracesInterceptor::class.java)
  }
}
