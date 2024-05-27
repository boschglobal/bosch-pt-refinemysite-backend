/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.EventConsumerRecord
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase

abstract class EventStreamContext(
    val events: MutableMap<String, SpecificRecordBase>,
    val lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    val timeLineGenerator: TimeLineGenerator,
    val listeners: MutableMap<String, List<KafkaListenerFunction>>,
    val businessTransactionsByReference: MutableMap<String, UUID> = mutableMapOf()
) {
  private var eventStream: MutableList<EventRecord> = ArrayList()

  private var nextOffset = 0L

  var lastBusinessTransactionReference: String? = null

  var lastRootContextIdentifier: AggregateIdentifierAvro? = null

  fun addEvent(context: String, reference: String, event: EventConsumerRecord) =
      eventStream.add(EventRecord(context, reference, event))

  fun getEventStream(): List<EventRecord> = eventStream.toList()

  fun getBusinessTransaction(reference: String) = businessTransactionsByReference[reference]

  fun registerBusinessTransaction(reference: String, transactionIdentifier: UUID = randomUUID()) {
    if (businessTransactionsByReference.containsKey(reference)) {
      error("There is already a business transaction registered for reference $reference")
    }
    businessTransactionsByReference[reference] = transactionIdentifier
  }

  fun nextOffset() = nextOffset++

  fun reset() {
    events.clear()
    lastBusinessTransactionReference = null
    lastRootContextIdentifier = null
    lastIdentifierPerType.clear()
    eventStream.clear()
    timeLineGenerator.reset()
    businessTransactionsByReference.clear()
    nextOffset = 0
  }

  abstract fun send(runnable: Runnable)
}

data class EventRecord(val context: String, val reference: String, val event: EventConsumerRecord)
