package com.bosch.pt.csm.cloud.event.application.config

import com.bosch.pt.csm.cloud.event.application.logging.MdcContextLifter
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
    val MDC_CONTEXT_REACTOR_KEY = MdcContextLifterConfiguration::class.java.name
  }
}
