/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextClosedEvent

@Configuration
@Profile("!test")
class ShutdownConfig(
    @Value("\${custom.shutdown.timeout-seconds:20}") private val timeoutSeconds: Long = 0
) : ApplicationListener<ContextClosedEvent> {

  override fun onApplicationEvent(event: ContextClosedEvent) {
    LOGGER.info(
        "Waiting {} seconds to let kubernetes pick up changed readiness state ...", timeoutSeconds)
    try {
      Thread.sleep(timeoutSeconds * 1000)
    } catch (e: InterruptedException) {
      LOGGER.error("The shutdown timeout has been interrupted")
      Thread.currentThread().interrupt()
    }
    LOGGER.info("Shutting down the application ...")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ShutdownConfig::class.java)
  }
}
