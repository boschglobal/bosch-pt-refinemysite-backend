/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.metrics

import com.bosch.pt.csm.company.eventstore.CompanyKafkaEventRepository
import io.micrometer.core.instrument.Metrics.gauge
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventTableMetrics(private val companyKafkaEventRepository: CompanyKafkaEventRepository) {

  private val numberOfEvents =
      gauge(
          "custom.eventtable.events.count", listOf(Tag.of("table", "company")), AtomicInteger(0))!!

  @Scheduled(fixedDelay = 5000L)
  fun updateMetrics() {
    numberOfEvents.set(companyKafkaEventRepository.count().toInt())
  }
}
