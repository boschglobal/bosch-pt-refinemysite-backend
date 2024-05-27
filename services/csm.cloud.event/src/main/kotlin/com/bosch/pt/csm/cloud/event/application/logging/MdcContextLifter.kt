/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.logging

import io.opentracing.Span
import kotlin.streams.asSequence
import org.reactivestreams.Subscription
import org.slf4j.MDC
import reactor.core.CoreSubscriber
import reactor.util.context.Context

class MdcContextLifter<T>(private val coreSubscriber: CoreSubscriber<T>) : CoreSubscriber<T> {

  override fun currentContext(): Context = coreSubscriber.currentContext()

  override fun onSubscribe(s: Subscription) = coreSubscriber.onSubscribe(s)

  override fun onError(t: Throwable) = coreSubscriber.onError(t)

  override fun onComplete() = coreSubscriber.onComplete()

  override fun onNext(t: T) {
    coreSubscriber.apply {
      copyToMdc(currentContext())
      onNext(t)
    }
  }

  private fun copyToMdc(context: Context) {
    if (!context.isEmpty) {
      val map =
          context.stream().asSequence().flatMap { mapEntry(it) }.associate { it.key to it.value }
      MDC.setContextMap(map)
    } else {
      MDC.clear()
    }
  }

  private fun mapEntry(entry: Map.Entry<Any, Any>): Sequence<Map.Entry<String, String?>> =
      if (entry.key == Span::class.java) {
        mapOf(
                "traceId" to (entry.value as Span).context().toTraceId(),
                "spanId" to (entry.value as Span).context().toSpanId())
            .asSequence()
      } else {
        mapOf(entry.key.toString() to entry.value.toString()).asSequence()
      }
}
