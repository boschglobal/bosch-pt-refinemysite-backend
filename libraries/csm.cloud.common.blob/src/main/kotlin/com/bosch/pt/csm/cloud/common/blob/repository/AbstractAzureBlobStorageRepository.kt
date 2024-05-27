/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.repository

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import io.opentracing.Span
import io.opentracing.log.Fields
import io.opentracing.tag.Tags
import io.opentracing.util.GlobalTracer

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractAzureBlobStorageRepository {

  fun trace(containerName: String, directory: String?, operationName: String, callback: (String) -> Blob): Blob {
    val tracer = GlobalTracer.get()
    val parentSpan = tracer.activeSpan()
    val span =
        tracer
            .buildSpan(operationName)
            .withTag("storage.containerName", containerName)
            .withTag("storage.directory", directory)
            .asChildOf(parentSpan)
            .start()

    try {
      tracer.activateSpan(span).use {
        return callback(toTraceHeader(span, parentSpan))
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      markAsFailed(span, e)
      throw e
    } finally {
      span.finish()
    }
  }

  private fun toTraceHeader(currentSpan: Span, parentSpan: Span): String {
    val traceId = currentSpan.context().toTraceId()
    val spanId = currentSpan.context().toSpanId()
    val parentSpanId = parentSpan.context().toSpanId()

    // construct trace header format: {TraceId}-{SpanId}-{SamplingState}-{ParentSpanId}
    return "$traceId-$spanId-$TRACE_SAMPLING_STATE_ACCEPT-$parentSpanId"
  }

  private fun markAsFailed(span: Span, ex: Exception) {
    span.setTag(Tags.ERROR, true)
    span.log(mapOf(Fields.ERROR_OBJECT to ex))
  }

  companion object {
    const val TRACE_SAMPLING_STATE_ACCEPT = "1"
  }
}
