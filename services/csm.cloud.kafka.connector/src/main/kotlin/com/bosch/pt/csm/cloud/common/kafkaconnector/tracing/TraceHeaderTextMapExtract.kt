/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.tracing

import io.opentracing.propagation.TextMapExtract

class TraceHeaderTextMapExtract(traceHeader: String) : TextMapExtract {

  private val map: MutableMap<String, String>

  init {
    // the header is assumed to have the format:
    // {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
    val header = traceHeader.split("-")
    require(header.size == 4) { "Trace header is expected to consist of 4 parts" }

    val traceId = header[TRACE_ID_INDEX]
    val parentSpanId = if (header.size == 4) header[PARENT_SPAN_ID_INDEX] else header[SPAN_ID_INDEX]

    map = mutableMapOf(DD_TRACE_ID_KEY to traceId, DD_PARENT_SPAN_ID_KEY to parentSpanId)
  }

  override fun iterator() = map.entries.iterator()

  companion object {
    private const val TRACE_ID_INDEX = 0
    private const val SPAN_ID_INDEX = 1
    private const val PARENT_SPAN_ID_INDEX = 3

    private const val DD_TRACE_ID_KEY = "x-datadog-trace-id"
    private const val DD_PARENT_SPAN_ID_KEY = "x-datadog-parent-id"
  }
}
