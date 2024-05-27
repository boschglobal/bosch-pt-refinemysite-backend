/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.metrics

import com.bosch.pt.csm.cloud.application.TestApplication
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import com.bosch.pt.csm.cloud.common.businesstransaction.metrics.BusinessTransactionMetrics.Companion.AGE_OF_OLDEST_EVENT_METRIC
import com.bosch.pt.csm.cloud.common.businesstransaction.metrics.BusinessTransactionMetrics.Companion.EVENT_PROCESSOR_TAG
import com.bosch.pt.csm.cloud.common.businesstransaction.metrics.BusinessTransactionMetricsTest.TestEventProcessor
import io.micrometer.core.instrument.MeterRegistry
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [TestApplication::class])
@Import(TestEventProcessor::class)
internal class BusinessTransactionMetricsTest {

  @Autowired lateinit var businessTransactionMetrics: BusinessTransactionMetrics

  @Autowired
  lateinit var eventOfBusinessTransactionRepository: EventOfBusinessTransactionRepositoryPort

  @Autowired lateinit var eventProcessors: List<BusinessTransactionAware>

  @Autowired lateinit var meterRegistry: MeterRegistry

  @Test
  fun `for each event processor there is exactly one gauge metric`() {
    val gauges = meterRegistry.find(AGE_OF_OLDEST_EVENT_METRIC).gauges()

    assertThat(gauges.count()).isEqualTo(eventProcessors.count())
  }

  @Test
  @Transactional
  fun `the gauge metric reflects the age of the oldest event`() {
    val gauge = findGaugeForEventProcessor(TestEventProcessor.NAME)

    businessTransactionMetrics.updateMetric()
    assertThat(gauge.value()).isEqualTo(0.0)

    val transactionIdentifier = randomUUID()

    eventOfBusinessTransactionRepository.save(
        randomEventOfBusinessTransaction(
            1, LocalDateTime.now().minusSeconds(100), transactionIdentifier))
    businessTransactionMetrics.updateMetric()
    assertThat(gauge.value()).isCloseTo(100.0, offset(10.0))

    eventOfBusinessTransactionRepository.save(
        randomEventOfBusinessTransaction(
            0, LocalDateTime.now().minusSeconds(200), transactionIdentifier))
    businessTransactionMetrics.updateMetric()
    assertThat(gauge.value()).isCloseTo(200.0, offset(10.0))

    eventOfBusinessTransactionRepository.removeAllByTransactionIdentifierAndEventProcessorName(
        transactionIdentifier, TestEventProcessor.NAME)
  }

  private fun findGaugeForEventProcessor(eventProcessorName: String) =
      meterRegistry.find(AGE_OF_OLDEST_EVENT_METRIC).gauges().single {
        it.id.getTag(EVENT_PROCESSOR_TAG) == eventProcessorName
      }

  private fun randomEventOfBusinessTransaction(
      offset: Long,
      creationDate: LocalDateTime,
      transactionIdentifier: UUID = randomUUID(),
      eventProcessorName: String = TestEventProcessor.NAME
  ) =
      EventOfBusinessTransaction(
          creationDate,
          LocalDateTime.now(),
          offset,
          transactionIdentifier,
          randomUUID().toString().toByteArray(),
          randomUUID().toString().toByteArray(),
          randomUUID().toString(),
          randomUUID().toString(),
          eventProcessorName)

  @Component
  class TestEventProcessor : BusinessTransactionAware {
    override fun getProcessorName() = NAME

    companion object {
      const val NAME = "test-processor"
    }
  }
}
