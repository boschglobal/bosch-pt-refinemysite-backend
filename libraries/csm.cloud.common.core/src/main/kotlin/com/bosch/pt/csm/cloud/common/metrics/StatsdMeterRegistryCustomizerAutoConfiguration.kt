/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.metrics

import io.micrometer.statsd.StatsdMeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(StatsdMeterRegistry::class)
@ConditionalOnEnabledMetricsExport("statsd")
class StatsdMeterRegistryCustomizerAutoConfiguration(
    @Value("\${DD_SERVICE:}") private val service: String,
    @Value("\${DD_VERSION:}") private val version: String,
    @Value("\${pod.name:}") private val podName: String
) {

  @Bean
  @ConditionalOnMissingBean(StatsdMeterRegistryCustomizer::class)
  fun statsdMeterRegistryCustomizer(): StatsdMeterRegistryCustomizer {
    if (service.isEmpty()) {
      LOGGER.error("Could not determine service name to add as tag value")
    }
    if (version.isEmpty()) {
      LOGGER.error("Could not determine version to add as tag value")
    }
    if (podName.isEmpty()) {
      LOGGER.error("Could not determine pod name to add as tag value")
    }
    return StatsdMeterRegistryCustomizer(service, version, podName)
  }

  companion object {
    val LOGGER: Logger = getLogger(StatsdMeterRegistryCustomizerAutoConfiguration::class.java)
  }
}
