/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.metrics

import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.UserContextKafkaEventRepository
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventTableMetrics(private val userContextKafkaEventRepository: UserContextKafkaEventRepository) {

  private val numberOfEvents =
      Metrics.gauge(
          "custom.eventtable.events.count", listOf(Tag.of("table", "user")), AtomicInteger(0))!!

  @Scheduled(fixedDelay = 5000L)
  fun updateMetrics() = numberOfEvents.set(userContextKafkaEventRepository.count().toInt())
}
