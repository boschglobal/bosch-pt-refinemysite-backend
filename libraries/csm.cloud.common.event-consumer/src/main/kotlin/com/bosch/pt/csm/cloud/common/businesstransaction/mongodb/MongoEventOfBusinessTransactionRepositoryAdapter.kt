/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.mongodb

import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

open class MongoEventOfBusinessTransactionRepositoryAdapter(
    private val repository: MongoEventOfBusinessTransactionRepository
) : EventOfBusinessTransactionRepositoryPort {

  open override fun countAllByCreationDateLessThan(creationDate: LocalDateTime) =
      repository.countAllByCreationDateLessThan(creationDate)

  open override fun findFirstByEventProcessorNameOrderByOffsetAsc(
      eventProcessorName: String
  ): EventOfBusinessTransaction? =
      repository.findFirstByEventProcessorNameOrderByOffsetAsc(eventProcessorName)?.toDto()

  open override fun findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
      transactionId: UUID,
      eventProcessorName: String
  ): List<EventOfBusinessTransaction> =
      repository
          .findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
              transactionId, eventProcessorName)
          .map { it.toDto() }
          .toList()

  open override fun removeAllByTransactionIdentifierAndEventProcessorName(
      transactionId: UUID,
      eventProcessorName: String
  ) =
      repository.removeAllByTransactionIdentifierAndEventProcessorName(
          transactionId, eventProcessorName)

  open override fun save(eventOfBusinessTransaction: EventOfBusinessTransaction) {
    val document = eventOfBusinessTransaction.toDocument()
    repository.save(document)
  }

  override fun deleteAll() = repository.deleteAll()

  private fun EventOfBusinessTransaction.toDocument() =
      EventsOfBusinessTransactionDocument(
          randomUUID(),
          creationDate,
          messageDate,
          offset,
          transactionIdentifier,
          eventProcessorName,
          Event(eventKey, eventValue, eventKeyClass, eventValueClass))

  private fun EventsOfBusinessTransactionDocument.toDto(): EventOfBusinessTransaction =
      EventOfBusinessTransaction(
          creationDate,
          messageDate,
          offset,
          transactionIdentifier,
          event.eventKey,
          event.eventValue,
          event.eventKeyClass,
          event.eventValueClass,
          eventProcessorName)
}
