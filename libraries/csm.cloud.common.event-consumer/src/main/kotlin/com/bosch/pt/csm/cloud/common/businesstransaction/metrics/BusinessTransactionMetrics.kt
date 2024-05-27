/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.businesstransaction.metrics

import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.annotation.PostConstruct
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong
import org.springframework.scheduling.annotation.Scheduled

open class BusinessTransactionMetrics(
    private val eventProcessors: List<BusinessTransactionAware>,
    private val eventOfBusinessTransactionRepository: EventOfBusinessTransactionRepositoryPort,
    private val meterRegistry: MeterRegistry
) {

  private lateinit var ageOfOldestEventByEventProcessor: Map<String, AtomicLong?>

  @PostConstruct
  fun registerMetrics() {
    ageOfOldestEventByEventProcessor =
        eventProcessors
            .associateBy { it.getProcessorName() }
            .mapValues {
              meterRegistry.gauge(
                  AGE_OF_OLDEST_EVENT_METRIC,
                  Tags.of("processor", it.value.getProcessorName()),
                  AtomicLong(0))
            }
  }

  @Scheduled(fixedDelay = 5000L)
  open fun updateMetric() {
    eventProcessors.forEach {
      val processorName = it.getProcessorName()
      val oldestEvent =
          eventOfBusinessTransactionRepository.findFirstByEventProcessorNameOrderByOffsetAsc(
              processorName)
      ageOfOldestEventByEventProcessor[processorName]!!.set(oldestEvent?.ageInSeconds() ?: 0L)
    }
  }

  private fun EventOfBusinessTransaction.ageInSeconds() =
      SECONDS.between(this.creationDate, LocalDateTime.now())

  companion object {
    const val AGE_OF_OLDEST_EVENT_METRIC = "custom.businesstransaction.events.oldest.age"
    const val EVENT_PROCESSOR_TAG = "processor"
  }
}
