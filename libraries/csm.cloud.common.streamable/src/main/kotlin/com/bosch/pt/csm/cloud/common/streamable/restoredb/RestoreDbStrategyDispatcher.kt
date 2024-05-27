/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Deprecated("To be replaced with new architecture")
class RestoreDbStrategyDispatcher<T : RestoreDbStrategy>(
    private val transactionTemplate: TransactionTemplate,
    private val strategies: List<T>
) {

  @Suppress("UNCHECKED_CAST")
  fun dispatch(record: ConsumerRecord<out EventMessageKey, SpecificRecordBase?>) {
    if (record.key() !is AggregateEventMessageKey) {
      return
    }

    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    val typedRecord = record as ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
    val handled = AtomicBoolean()
    transactionTemplate.executeWithoutResult {
      strategies
          .filter { strategy -> strategy.canHandle(typedRecord) }
          .forEach { strategy ->
            handled.set(true)
            strategy.handle(typedRecord)
          }
    }

    require(handled.get()) {
      ("No strategy found to handle event of type: " + typedRecord.key().aggregateIdentifier.type)
    }
  }
}
