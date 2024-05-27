/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.metrics

import io.micrometer.statsd.StatsdMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer

class StatsdMeterRegistryCustomizer(
    private val service: String,
    private val version: String,
    private val podName: String
) : MeterRegistryCustomizer<StatsdMeterRegistry> {

  override fun customize(registry: StatsdMeterRegistry) {
    registry.config().commonTags("service", service, "version", version, "pod_name", podName)
  }
}
