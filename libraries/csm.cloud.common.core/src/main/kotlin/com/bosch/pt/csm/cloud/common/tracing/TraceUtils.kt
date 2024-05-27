/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.tracing

import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.log.Fields
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer
import java.util.function.BiFunction

object TraceUtils {
  fun <T> traceWithNewSpan(
      operationName: String,
      tags: Map<String, String>? = null,
      function: BiFunction<Tracer, Span, T>
  ): T {
    val tracer = GlobalTracer.get()
    val parentSpan = tracer.activeSpan()
    val spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpan)
    tags?.entries?.forEach { spanBuilder.withTag(it.key, it.value) }

    val span = spanBuilder.start()
    try {
      return function.apply(tracer, span)
    } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
      markAsFailed(span, ex)
      throw ex
    } finally {
      span.finish()
    }
  }

  private fun markAsFailed(span: Span, ex: Exception) =
      span.apply {
        setTag(Tags.ERROR, true)
        log(mapOf(Fields.ERROR_OBJECT to ex))
      }
}
