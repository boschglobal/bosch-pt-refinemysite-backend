/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.logging

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.springframework.context.annotation.Configuration
import reactor.core.CoreSubscriber
import reactor.core.Scannable
import reactor.core.publisher.Hooks
import reactor.core.publisher.Operators

@Configuration
class MdcContextLifterConfiguration {

  @PostConstruct
  fun contextOperatorHook() =
      Hooks.onEachOperator(
          MDC_CONTEXT_REACTOR_KEY,
          Operators.lift { _: Scannable, subscriber: CoreSubscriber<in Any> ->
            MdcContextLifter(subscriber)
          })

  @PreDestroy fun cleanupHook() = Hooks.resetOnEachOperator(MDC_CONTEXT_REACTOR_KEY)

  companion object {
    val MDC_CONTEXT_REACTOR_KEY: String = MdcContextLifterConfiguration::class.java.name
  }
}
