/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.ConsumerBusinessTransactionManager
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.getTransactionIdentifier
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.isBusinessTransactionFinished
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.isBusinessTransactionStarted
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.isPartOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.model.key.BusinessTransactionStartedMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractBusinessTransactionAwareListener(
    private val businessTransactionManager: ConsumerBusinessTransactionManager,
    private val eventProcessor: BusinessTransactionAware
) {

  fun process(record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>) {
    require(isActualTransactionActive()) { "An active database transaction is required." }

    val processorName = eventProcessor.getProcessorName()
    when {
      record.isBusinessTransactionStarted() -> {
        val transactionIdentifier = record.getTransactionIdentifierOrFail()
        val events = readEventsFromDatabaseRemovingDuplicates(transactionIdentifier, processorName)

        if (events.isEmpty()) {
          businessTransactionManager.saveEventToDatabase(record, processorName)
          eventProcessor.onTransactionStarted(record.toEventRecord())
        } else if (events.firstIsStartedEvent()) {
          // skip duplicated started event
          LOGGER.warn(
              "Found business transaction $transactionIdentifier with duplicated 'started' event. To ensure " +
                  "idempotency, the duplicated event will be skipped.")
        } else {
          error(
              "Found business transaction 'started' event which is not the first event in " +
                  "business transaction $transactionIdentifier")
        }
      }
      record.isBusinessTransactionFinished() -> {
        val transactionIdentifier = record.getTransactionIdentifierOrFail()
        val events = readEventsFromDatabaseRemovingDuplicates(transactionIdentifier, processorName)

        if (events.firstIsStartedEvent()) {
          eventProcessor.onTransactionFinished(
              transactionStartedRecord = events.first(),
              events = events.subList(1, events.size),
              transactionFinishedRecord = record.toEventRecord())
        } else {
          // skip duplicated finished event
          LOGGER.warn(
              "Found business transaction $transactionIdentifier without a 'started' event. This is most likely " +
                  "caused by a duplicated 'finished' event in the event stream. To ensure idempotency, " +
                  "all ${events.size + 1} event(s) from the duplicated business transaction will be skipped.")
        }

        businessTransactionManager.removeEventsFromDatabase(transactionIdentifier, processorName)
      }
      // do not reorder this branch! It must not come before the started/finished branch because
      // started/finished events are also "part of" a business transaction.
      record.isPartOfBusinessTransaction() -> {
        // in future, we might want to save only events the listener is really interested in
        businessTransactionManager.saveEventToDatabase(record, processorName)
        eventProcessor.onTransactionalEvent(record.toEventRecord())
      }
      else -> eventProcessor.onNonTransactionalEvent(record.toEventRecord())
    }
  }

  private fun readEventsFromDatabaseRemovingDuplicates(
      transactionIdentifier: UUID,
      processorName: String
  ) =
      businessTransactionManager
          .readEventsFromDatabase(transactionIdentifier, processorName)
          // remove duplicates to ensure idempotency (for a duplicate, the first is kept)
          .distinctBy { Pair(it.key, it.value) }

  private fun List<EventRecord>.firstIsStartedEvent() =
      this.isNotEmpty() && this.first().key is BusinessTransactionStartedMessageKey

  private fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>
      .getTransactionIdentifierOrFail() =
      this.getTransactionIdentifier()
          ?: error("Failed to get transaction identifier of record $this")

  private fun ConsumerRecord<EventMessageKey, SpecificRecordBase?>.toEventRecord() =
      EventRecord(this.key(), this.value(), this.timestamp().toLocalDateTimeByMillis())

  companion object {
    val LOGGER: Logger =
        LoggerFactory.getLogger(AbstractBusinessTransactionAwareListener::class.java)
  }
}
