/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.application.config

import com.bosch.pt.csm.cloud.pdf.application.pdf.PlaywrightFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextClosedEvent

@Configuration
@Profile("!test")
class ShutdownConfig(
    private val playwrightFactory: PlaywrightFactory,
    @Value("\${custom.shutdown.timeout-seconds:20}") private val timeoutSeconds: Long
) : ApplicationListener<ContextClosedEvent> {

  override fun onApplicationEvent(event: ContextClosedEvent) {
    LOGGER.info(
        "Waiting $timeoutSeconds seconds to let kubernetes pick up changed readiness state ...")

    LOGGER.info("Stop playwright ...")
    try {
      playwrightFactory.close()
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      LOGGER.warn("Shutdown playwright failed", e)
    }

    LOGGER.info("Delay shutdown ...")
    try {
      Thread.sleep(timeoutSeconds * 1000)
    } catch (e: InterruptedException) {
      LOGGER.error("The shutdown timeout has been interrupted")
    }
    LOGGER.info("Shutting down the application ...")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ShutdownConfig::class.java)
  }
}
