/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.getFieldByPath
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Optional
import java.util.UUID
import org.apache.avro.Conversions
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecord.NULL_SIZE
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.slf4j.LoggerFactory.getLogger
import org.springframework.util.Assert

class EventStreamGenerator(private val context: EventStreamContext) {

  private var defaultUserName: String? = null

  fun setUserContext(name: String): EventStreamGenerator {
    defaultUserName = name
    return this
  }

  fun getUserName(name: String): String =
      when (name) {
        DEFAULT_USER -> defaultUserName!!
        else -> name
      }

  fun setLastIdentifierForType(
      type: String,
      identifier: AggregateIdentifierAvro
  ): EventStreamGenerator {
    getContext().lastIdentifierPerType[type] = identifier
    return this
  }

  fun reset(): EventStreamGenerator {
    context.reset()
    return this
  }

  fun getContext(): EventStreamContext = context

  fun AggregateIdentifierAvro.Builder.increase(eventType: String): AggregateIdentifierAvro.Builder =
      when (eventType) {
        "CREATED" -> this
        else -> setVersion(version + 1)
      }

  fun getAggregateIdentifier(aggregate: SpecificRecordBase) =
      aggregate.get("aggregateIdentifier") as AggregateIdentifierAvro

  fun setAuditingInformation(
      auditingInformation: AuditingInformationAvro.Builder,
      eventType: String,
      auditUserReference: String?,
      time: Instant
  ) {
    val user = auditUserReference?.let { get<SpecificRecordBase?>(getUserName(it)) }

    when (eventType) {
      "CREATED" -> {
        user?.apply { auditingInformation.createdBy = getAggregateIdentifier(this) }
        auditingInformation.createdDate = time.toEpochMilli()
      }
    }

    user?.apply { auditingInformation.lastModifiedBy = getAggregateIdentifier(this) }
    auditingInformation.lastModifiedDate = time.toEpochMilli()
  }

  fun setEventAuditingInformation(
      builder: EventAuditingInformationAvro.Builder,
      user: String,
      date: Instant
  ) {
    val userAggregate =
        get<SpecificRecordBase?>(getUserName(user)) ?: error("No user found for reference $user")

    builder.date = date.toEpochMilli()
    builder.user = getAggregateIdentifier(userAggregate).getIdentifier()
  }

  // Return a (deep) copy of the referenced object. This ensures that already sent events that were
  // collected are not modified
  @Suppress("UNCHECKED_CAST")
  fun <T : SpecificRecordBase?> get(name: String): T? =
      (getContext().events[name] as T)?.let { deepCopy(it) }

  fun send(
      context: String,
      reference: String,
      messageKey: EventMessageKey?,
      value: SpecificRecordBase,
      timestamp: Long = LocalDateTime.now().toInstant(UTC).toEpochMilli(),
      businessTransactionReference: String? = null
  ): SpecificRecordBase? {
    val consumerRecord =
        mockConsumerRecord(
            messageKey ?: value.deriveAggregateEventMessageKey(),
            value,
            timestamp,
            mockHeaders(businessTransactionReference))
    LOGGER.debug("Sending record: $consumerRecord")

    findListenersOrFail(context).forEach {
      getContext().send {
        val ack = TestAcknowledgement()
        it(consumerRecord, ack)
        Assert.isTrue(ack.isAcknowledged, "Processing of kafka message failed")
      }
    }

    if (value.isAggregateEvent()) {
      value.extractAggregateIdentifier().also { getContext().lastIdentifierPerType[it.type] = it }
    }
    getContext().addEvent(context, reference, consumerRecord)

    return consumerRecord.value()
  }

  private fun findListenersOrFail(context: String): List<KafkaListenerFunction> {
    val listeners = getContext().listeners[context] ?: emptyList()
    Assert.notEmpty(listeners, "No kafka listener for the \"$context\" context is registered")
    return listeners
  }

  fun mockHeaders(businessTransactionReference: String?): Iterable<Header> {
    if (businessTransactionReference == null) {
      return listOf()
    }
    val businessTransactionIdentifier =
        getContext().getBusinessTransaction(businessTransactionReference)
            ?: error(
                "No business transaction found for reference \"$businessTransactionReference.\"")

    return listOf(
        RecordHeader(
            "businessTransactionId", businessTransactionIdentifier.toString().toByteArray()))
  }

  fun sendTombstoneMessage(
      context: String,
      reference: String,
      messageKey: EventMessageKey,
      timestamp: Long = LocalDateTime.now().toInstant(UTC).toEpochMilli()
  ) {
    val consumerRecord = mockConsumerRecord(messageKey, null, timestamp)

    findListenersOrFail(context).forEach {
      getContext().send {
        val ack = TestAcknowledgement()
        it.invoke(consumerRecord, ack)
        Assert.isTrue(ack.isAcknowledged, "Processing of kafka message failed")
      }

      getContext().addEvent(context, reference, consumerRecord)
    }
  }

  fun getEventHistory(
      reference: String
  ): List<ConsumerRecord<EventMessageKey, SpecificRecordBase?>> =
      context.getEventStream().filter { it.reference == reference }.map { it.event }

  fun <T> getEventFieldHistory(reference: String, vararg fieldPath: String): List<T> =
      getEventHistory(reference).mapNotNull { it.value()?.getFieldByPath(*fieldPath) }

  fun <T> getEventFieldOrNull(reference: String, vararg fieldPath: String): T? =
      getEventFieldHistory<T>(reference, *fieldPath).lastOrNull()

  fun <T> getEventField(reference: String, vararg fieldPath: String): T =
      getEventFieldOrNull(reference, *fieldPath)
          ?: throw IllegalStateException(
              "No value found for field \"${fieldPath.joinToString(".")}\"" +
                  " in event stream history of event \"$reference\"")

  fun repeat(lastEvents: Int = 0): EventStreamGenerator {
    val numberOfEvents = context.getEventStream().size - lastEvents
    LOGGER.warn("Repeating last $lastEvents of $numberOfEvents events...")
    context.getEventStream().takeLast(lastEvents).forEach { (context, _, record) ->
      val listeners = getContext().listeners[context] ?: emptyList()
      listeners.forEach {
        getContext().send {
          val ack = TestAcknowledgement()
          it(record, ack)
          check(ack.isAcknowledged) { "Processing of kafka message failed" }
        }
      }
    }
    return this
  }

  fun repeat(runnable: Runnable) {
    val existingEventCount = context.getEventStream().size
    runnable.run()
    val numberEvents = context.getEventStream().size - existingEventCount
    repeat(numberEvents)
  }

  fun getActiveUserFromContext(): AggregateIdentifierAvro =
      defaultUserName!!.let { getContext().events[defaultUserName]!!.extractAggregateIdentifier() }

  fun getByReference(reference: String): AggregateIdentifierAvro =
      getContext().events[reference]?.extractAggregateIdentifier()
          ?: error(
              "Could not find reference: \"$reference\". " +
                  "Either there's a typo in the reference name or the object does not exist with this reference name.")

  fun getIdentifier(reference: String): UUID = getByReference(reference).identifier.toUUID()

  private fun mockConsumerRecord(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      timestamp: Long,
      headers: Iterable<Header> = listOf()
  ): ConsumerRecord<EventMessageKey, SpecificRecordBase?> =
      ConsumerRecord(
          "",
          0,
          context.nextOffset(),
          timestamp,
          TimestampType.CREATE_TIME,
          NULL_SIZE,
          NULL_SIZE,
          key,
          value,
          RecordHeaders(headers),
          Optional.empty(),
      )

  private fun <T : SpecificRecordBase> deepCopy(obj: T): T {
    val specificData = SpecificData.get()
    specificData.addLogicalTypeConversion(Conversions.DecimalConversion())
    return specificData.deepCopy(obj.schema, obj)
  }

  private fun SpecificRecordBase.deriveAggregateEventMessageKey(): AggregateEventMessageKey =
      if (this.isAggregateEvent()) {
        this.extractAggregateIdentifier().let {
          AggregateEventMessageKey(it.buildAggregateIdentifier(), it.identifier.toUUID())
        }
      } else {
        throw IllegalStateException(
            "Deriving the message key from an event is supported only for aggregate events. " +
                "This does not look like an agregate event: $this")
      }

  /**
   * This method extract the aggregate identifier from a [SpecificRecordBase]. The assumption here
   * is that we either have an instance of [SpecificRecordBase] that contains a field "aggregate"
   * with a nested "aggregateIdentifier" field, or it contains a "aggregateIdentifier" field on the
   * root level.
   *
   * The first scenario is valid for aggregate events transporting snapshots. The second scenario is
   * valid for event-sourcing events such as job events.
   */
  private fun SpecificRecordBase.extractAggregateIdentifier(): AggregateIdentifierAvro =
      when {
        this.hasField("aggregate") ->
            (this.get("aggregate") as SpecificRecordBase).extractAggregateIdentifier()
        this.hasField("aggregateIdentifier") ->
            this.get("aggregateIdentifier") as AggregateIdentifierAvro
        else ->
            throw IllegalStateException(
                "Unable to extract aggregate identifier from record. " +
                    "Expected either an \"aggregate\" or \"aggregateIdentifier\" field: $this")
      }

  private fun SpecificRecordBase.isAggregateEvent() =
      hasField("aggregate") || hasField("aggregateIdentifier")

  companion object {
    const val DEFAULT_USER = "<default>"

    private val LOGGER = getLogger(EventStreamGenerator::class.java)

    fun newAggregateIdentifier(
        type: String,
        identifier: UUID = UUID.randomUUID()
    ): AggregateIdentifierAvro.Builder =
        AggregateIdentifierAvro.newBuilder()
            .setIdentifier(identifier.toString())
            .setType(type)
            .setVersion(0)

    fun newAuditingInformation(): AuditingInformationAvro.Builder =
        AuditingInformationAvro.newBuilder()
  }
}
