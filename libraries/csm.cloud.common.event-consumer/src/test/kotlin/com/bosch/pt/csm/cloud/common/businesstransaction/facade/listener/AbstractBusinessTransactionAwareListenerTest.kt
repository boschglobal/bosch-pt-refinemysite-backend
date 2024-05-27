/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener

import com.bosch.pt.csm.cloud.application.MySqlTest
import com.bosch.pt.csm.cloud.application.TestApplication
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.ConsumerBusinessTransactionManager
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.AbstractBusinessTransactionAwareListenerTest.TestBusinessTransactionAwareListener
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionFinishedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.util.KafkaTestUtils.mockRecord
import com.ninjasquad.springmockk.SpykBean
import io.mockk.excludeRecords
import io.mockk.verifySequence
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
@ContextConfiguration(classes = [TestApplication::class])
@Import(TestBusinessTransactionAwareListener::class)
@MySqlTest
internal class AbstractBusinessTransactionAwareListenerTest {

  @Autowired private lateinit var cut: TestBusinessTransactionAwareListener

  @Autowired
  private lateinit var businessTransactionRepository: EventOfBusinessTransactionRepositoryPort

  @Autowired private lateinit var transactionTemplate: TransactionTemplate

  @SpykBean private lateinit var eventProcessor: TestEventProcessor

  @BeforeEach
  fun init() {
    excludeRecords { eventProcessor.getProcessorName() }
  }

  @AfterEach fun cleanUp() = businessTransactionRepository.deleteAll()

  @Test
  fun `transaction started events are dispatched`() {
    val record = mockStartedRecord(0)

    transactionTemplate.execute { cut.process(record) }

    verifySequence { eventProcessor.onTransactionStarted(any()) }
  }

  @Test
  fun `transaction finished events are dispatched`() {
    val transactionIdentifier = randomUUID()
    val startedRecord = mockStartedRecord(0, transactionIdentifier)
    val finishedRecord = mockFinishedRecord(1, transactionIdentifier)

    transactionTemplate.execute {
      cut.process(startedRecord)
      cut.process(finishedRecord)
    }

    verifySequence {
      eventProcessor.onTransactionStarted(any())
      eventProcessor.onTransactionFinished(any(), any(), any())
    }
  }

  @Test
  fun `verify idempotency on duplicated business transaction started event`() {
    val transactionIdentifier = randomUUID()
    val startedRecord = mockStartedRecord(0, transactionIdentifier)
    val middleRecord = mockRecord(1, transactionIdentifier = transactionIdentifier)
    val finishedRecord = mockFinishedRecord(2, transactionIdentifier)

    transactionTemplate.execute {
      cut.process(startedRecord)
      cut.process(startedRecord) // <-- duplicate
      cut.process(middleRecord)
      cut.process(finishedRecord)
    }

    // do not inline, there seems to be a bug with mockk when extension functions are invoked inside
    // a verify block
    val expectedStartedRecord = startedRecord.toEventRecord()
    val expectedMiddleRecord = middleRecord.toEventRecord()
    val expectedFinishedRecord = finishedRecord.toEventRecord()
    val expectedEvents = listOf(middleRecord.toEventRecord())

    verifySequence {
      eventProcessor.onTransactionStarted(expectedStartedRecord)
      eventProcessor.onTransactionalEvent(expectedMiddleRecord)
      eventProcessor.onTransactionFinished(
          expectedStartedRecord, expectedEvents, expectedFinishedRecord)
    }
  }

  @Test
  fun `verify idempotency on duplicated business transaction finished event`() {
    val transactionIdentifier = randomUUID()
    val startedRecord = mockStartedRecord(0, transactionIdentifier)
    val middleRecord = mockRecord(1, transactionIdentifier = transactionIdentifier)
    val finishedRecord = mockFinishedRecord(2, transactionIdentifier)

    transactionTemplate.execute {
      cut.process(startedRecord)
      cut.process(middleRecord)
      cut.process(finishedRecord)
      cut.process(finishedRecord) // <-- duplicate
    }

    // do not inline, there seems to be a bug with mockk when extension functions are invoked inside
    // a verify block
    val expectedStartedRecord = startedRecord.toEventRecord()
    val expectedMiddleRecord = middleRecord.toEventRecord()
    val expectedFinishedRecord = finishedRecord.toEventRecord()
    val expectedEvents = listOf(middleRecord.toEventRecord())

    verifySequence {
      eventProcessor.onTransactionStarted(expectedStartedRecord)
      eventProcessor.onTransactionalEvent(expectedMiddleRecord)
      eventProcessor.onTransactionFinished(
          expectedStartedRecord, expectedEvents, expectedFinishedRecord)
    }
  }

  @Test
  fun `verify no idempotency for duplicated events except for started and finished events`() {
    val transactionIdentifier = randomUUID()
    val startedRecord = mockStartedRecord(0, transactionIdentifier)
    val firstMiddleRecord = mockRecord(1, transactionIdentifier = transactionIdentifier)
    val secondMiddleRecord = mockRecord(2, transactionIdentifier = transactionIdentifier)
    val finishedRecord = mockFinishedRecord(3, transactionIdentifier)

    transactionTemplate.execute {
      cut.process(startedRecord)
      cut.process(firstMiddleRecord)
      cut.process(secondMiddleRecord)
      cut.process(firstMiddleRecord) // <-- duplicate
      cut.process(secondMiddleRecord) // <-- duplicate
      cut.process(finishedRecord)
    }

    // do not inline, there seems to be a bug with mockk when extension functions are invoked inside
    // a verify block
    val expectedStartedRecord = startedRecord.toEventRecord()
    val expectedFirstMiddleRecord = firstMiddleRecord.toEventRecord()
    val expectedSecondsMiddleRecord = secondMiddleRecord.toEventRecord()
    val expectedFinishedRecord = finishedRecord.toEventRecord()
    val expectedEvents =
        listOf(firstMiddleRecord.toEventRecord(), secondMiddleRecord.toEventRecord())

    verifySequence {
      eventProcessor.onTransactionStarted(expectedStartedRecord)
      eventProcessor.onTransactionalEvent(expectedFirstMiddleRecord)
      eventProcessor.onTransactionalEvent(expectedSecondsMiddleRecord)
      eventProcessor.onTransactionalEvent(expectedFirstMiddleRecord)
      eventProcessor.onTransactionalEvent(expectedSecondsMiddleRecord)
      eventProcessor.onTransactionFinished(
          expectedStartedRecord, expectedEvents, expectedFinishedRecord)
    }
  }

  @Test
  fun `on transaction finished, events are dispatched in order of occurrence`() {
    val transactionIdentifier = randomUUID()
    val startedRecord = mockStartedRecord(0, transactionIdentifier)
    val middleRecord = mockRecord(1, transactionIdentifier = transactionIdentifier)
    val finishedRecord = mockFinishedRecord(2, transactionIdentifier)

    transactionTemplate.execute {
      cut.process(startedRecord)
      cut.process(middleRecord)
      cut.process(finishedRecord)
    }

    // do not inline, there seems to be a bug with mockk when extension functions are invoked inside
    // a verify block
    val expectedStartedRecord = startedRecord.toEventRecord()
    val expectedMiddleRecord = middleRecord.toEventRecord()
    val expectedFinishedRecord = finishedRecord.toEventRecord()
    val expectedEvents = listOf(middleRecord.toEventRecord())

    verifySequence {
      eventProcessor.onTransactionStarted(expectedStartedRecord)
      eventProcessor.onTransactionalEvent(expectedMiddleRecord)
      eventProcessor.onTransactionFinished(
          expectedStartedRecord, expectedEvents, expectedFinishedRecord)
    }
  }

  @Test
  fun `transactional events are dispatched`() {
    val record = mockRecord(0, transactionIdentifier = randomUUID())

    transactionTemplate.execute { cut.process(record) }

    val expectedRecord = record.toEventRecord()
    verifySequence { eventProcessor.onTransactionalEvent(expectedRecord) }
  }

  @Test
  fun `non-transactional events are dispatched`() {
    val record = mockRecord(0)

    transactionTemplate.execute { cut.process(record) }

    verifySequence { eventProcessor.onNonTransactionalEvent(any()) }
  }

  private fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.toEventRecord() =
      EventRecord(this.key(), this.value(), this.timestamp().toLocalDateTimeByMillis())

  private fun mockStartedRecord(offset: Long, transactionIdentifier: UUID = randomUUID()) =
      mockRecord(
          offset = offset,
          key = BusinessTransactionStartedMessageKey(transactionIdentifier, randomUUID()),
          transactionIdentifier = transactionIdentifier)

  private fun mockFinishedRecord(offset: Long, transactionIdentifier: UUID = randomUUID()) =
      mockRecord(
          offset = offset,
          key = BusinessTransactionFinishedMessageKey(transactionIdentifier, randomUUID()),
          transactionIdentifier = transactionIdentifier)

  @Component
  class TestBusinessTransactionAwareListener(
      businessTransactionManager: ConsumerBusinessTransactionManager,
      eventProcessor: TestEventProcessor
  ) : AbstractBusinessTransactionAwareListener(businessTransactionManager, eventProcessor)

  class TestEventProcessor : BusinessTransactionAware {
    override fun getProcessorName() = "test-processor"
  }
}
