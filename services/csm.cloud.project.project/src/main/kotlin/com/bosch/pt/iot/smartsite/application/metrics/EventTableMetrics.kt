/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.metrics

import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEventRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextKafkaEventRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventTableMetrics(
    private val projectEventStoreRepository: ProjectContextKafkaEventRepository,
    private val invitationEventStoreRepository: ProjectInvitationContextKafkaEventRepository,
    meterRegistry: MeterRegistry
) {

  private val numberOfProjectEvents =
      meterRegistry.gauge(
          "custom.eventtable.events.count", listOf(Tag.of("table", "project")), AtomicInteger(0))

  private val numberOfInvitationEvents =
      meterRegistry.gauge(
          "custom.eventtable.events.count", listOf(Tag.of("table", "invitation")), AtomicInteger(0))

  @Scheduled(fixedDelay = 5000L)
  fun updateMetrics() {
    numberOfProjectEvents?.set(projectEventStoreRepository.count().toInt())
    numberOfInvitationEvents?.set(invitationEventStoreRepository.count().toInt())
  }
}
