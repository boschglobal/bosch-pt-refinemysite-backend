/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.RESTORE
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.Assert

open class RestoreFromKafkaAdapter(
    private val transactionTemplate: TransactionTemplate,
    private val eventBus: LocalEventBus
) {

  protected fun emit(record: ConsumerRecord<out EventMessageKey, SpecificRecordBase?>) {

    // restore makes sense only for aggregate events; otherwise, ignore
    if (record.key() !is AggregateEventMessageKey) return

    Assert.isTrue(
        !TransactionSynchronizationManager.isActualTransactionActive(),
        "No running transaction expected")

    transactionTemplate.executeWithoutResult {
      val key = record.key() as AggregateEventMessageKey
      when {
        record.value() == null -> eventBus.emitTombstone(key, RESTORE)
        else -> eventBus.emit(key, record.value()!!, RESTORE)
      }
    }
  }
}
