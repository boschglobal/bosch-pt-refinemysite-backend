/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.boundary

import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.getTransactionIdentifier
import com.bosch.pt.csm.cloud.common.businesstransaction.extension.isPartOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.messages.MessageKeyFactory.createEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.annotation.Transactional

open class ConsumerBusinessTransactionManager(
    private val eventOfBusinessTransactionRepository: EventOfBusinessTransactionRepositoryPort
) {

  @Transactional
  open fun saveEventToDatabase(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      eventProcessorName: String
  ) {
    assertIsPartOfBusinessTransaction(record)
    assertIsNotTombstoneMessage(record)

    eventOfBusinessTransactionRepository.save(
        EventOfBusinessTransaction(
            LocalDateTime.now(),
            record.timestamp().toLocalDateTimeByMillis(),
            record.offset(),
            record.getTransactionIdentifier()!!,
            record.key().serialize(),
            record.value()!!.serialize(),
            record.key().toAvro().javaClass.name,
            record.value()!!.javaClass.name,
            eventProcessorName))
  }

  @Transactional(readOnly = true)
  open fun readEventsFromDatabase(
      transactionId: UUID,
      eventProcessorName: String
  ): List<EventRecord> =
      eventOfBusinessTransactionRepository
          .findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
              transactionId, eventProcessorName)
          // remove messages received more than once. Note: this does not guarantee that there are
          // no duplicate events. It can still happen that the same event is sent twice to Kafka,
          // each with a unique offset.
          .distinctBy { it.offset }
          .map {
            EventRecord(
                key = deserialize(it.eventKey, it.eventKeyClass).let { createEventMessageKey(it) },
                value = deserialize(it.eventValue, it.eventValueClass),
                messageDate = it.messageDate)
          }

  @Transactional
  open fun removeEventsFromDatabase(transactionId: UUID, eventProcessorName: String) =
      eventOfBusinessTransactionRepository.removeAllByTransactionIdentifierAndEventProcessorName(
          transactionId, eventProcessorName)

  private fun EventMessageKey.serialize(): ByteArray =
      (this.toAvro() as SpecificRecordBase).serialize()

  private fun SpecificRecordBase.serialize(): ByteArray {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    return oos.use {
      // write SpecificRecordBase content to the output stream
      writeExternal(it)
      oos.flush()
      baos.toByteArray()
    }
  }

  private fun deserialize(serializedEvent: ByteArray, className: String): SpecificRecordBase {
    val event = className.newInstanceForClassName() as SpecificRecordBase
    val ois = ObjectInputStream(serializedEvent.inputStream())
    ois.use { event.readExternal(ois) }
    return event
  }

  private fun String.newInstanceForClassName() =
      Class.forName(this).getDeclaredConstructor().newInstance()

  private fun assertIsNotTombstoneMessage(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>
  ) = require(record.value() != null) { "Record must not be a tombstone message" }

  private fun assertIsPartOfBusinessTransaction(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>
  ) =
      require(record.isPartOfBusinessTransaction()) {
        "Record must be part of a business transaction."
      }
}

data class EventRecord(
    val key: EventMessageKey,
    val value: SpecificRecordBase?,

    /**
     * the date and time this event was published as a message (this is the Kafka timestamp, it's
     * *not* the time the event occurred)
     */
    val messageDate: LocalDateTime
)
