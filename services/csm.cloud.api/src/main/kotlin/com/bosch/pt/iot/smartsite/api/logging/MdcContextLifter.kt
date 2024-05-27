/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.logging

import java.util.stream.Collectors.toMap
import java.util.stream.Stream
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
          context
              .stream()
              .map { Pair(it.key.toString(), it.value?.toString()) }
              .flatMap { Stream.of(it) }
              .collect(toMap({ (key, _) -> key }, { (_, value) -> value }))
      MDC.setContextMap(map)
    } else {
      MDC.clear()
    }
  }
}
