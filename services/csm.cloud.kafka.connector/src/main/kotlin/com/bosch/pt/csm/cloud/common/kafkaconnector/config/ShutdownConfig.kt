/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.config

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler

@Configuration
@Profile("!test")
class ShutdownConfig(
    private val scheduler: SimpleAsyncTaskScheduler,
    @Value("\${custom.shutdown.timeout-seconds}") private val timeoutSeconds: Long
) : ApplicationListener<ContextClosedEvent> {

  override fun onApplicationEvent(event: ContextClosedEvent) {
    LOGGER.info("Shutting down the application ...")

    if (scheduler.isActive) {
      LOGGER.info("Waiting for currently active task(s) to be completed ...")
      scheduler.setTaskTerminationTimeout(timeoutSeconds)
    }
    // if not called explicitly here, waiting for tasks to be completed does not work
    scheduler.close()
  }

  companion object {
    private val LOGGER = getLogger(ShutdownConfig::class.java)
  }
}
