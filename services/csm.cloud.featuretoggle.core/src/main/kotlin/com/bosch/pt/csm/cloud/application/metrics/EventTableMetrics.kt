/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.metrics

import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleContextKafkaEventRepository
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventTableMetrics(
    private val featuretoggleContextKafkaEventRepository: FeaturetoggleContextKafkaEventRepository
) {

  private val numberOfEvents =
      Metrics.gauge(
          "custom.eventtable.events.count", listOf(Tag.of("table", "featuretoggle")), AtomicInteger(0))!!

  @Scheduled(fixedDelay = 5000L)
  fun updateMetrics() = numberOfEvents.set(featuretoggleContextKafkaEventRepository.count().toInt())
}
