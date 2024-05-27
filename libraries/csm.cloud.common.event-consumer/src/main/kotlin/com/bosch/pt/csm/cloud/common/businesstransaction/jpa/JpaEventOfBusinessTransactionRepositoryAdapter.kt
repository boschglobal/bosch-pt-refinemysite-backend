/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.jpa

import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransaction
import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import java.time.LocalDateTime
import java.util.UUID

open class JpaEventOfBusinessTransactionRepositoryAdapter(
    private val repository: JpaEventOfBusinessTransactionRepository
) : EventOfBusinessTransactionRepositoryPort {

  open override fun countAllByCreationDateLessThan(creationDate: LocalDateTime) =
      repository.countAllByCreationDateLessThan(creationDate)

  open override fun findFirstByEventProcessorNameOrderByOffsetAsc(
      eventProcessorName: String
  ): EventOfBusinessTransaction? =
      repository.findFirstByEventProcessorNameOrderByConsumerOffsetAsc(eventProcessorName).let {
        it?.toDto()
      }

  open override fun findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
      transactionId: UUID,
      eventProcessorName: String
  ): List<EventOfBusinessTransaction> =
      repository
          .findAllByTransactionIdentifierAndEventProcessorName(transactionId, eventProcessorName)
          .sortedBy { it.consumerOffset }
          .map { it.toDto() }

  open override fun removeAllByTransactionIdentifierAndEventProcessorName(
      transactionId: UUID,
      eventProcessorName: String
  ) =
      repository.removeAllByTransactionIdentifierAndEventProcessorName(
          transactionId, eventProcessorName)

  open override fun save(eventOfBusinessTransaction: EventOfBusinessTransaction) {
    repository.save(eventOfBusinessTransaction.toEntity())
  }

  override fun deleteAll() = repository.deleteAll()

  private fun EventOfBusinessTransaction.toEntity() =
      EventOfBusinessTransactionEntity(
          creationDate,
          messageDate,
          offset,
          transactionIdentifier,
          eventKey,
          eventValue,
          eventKeyClass,
          eventValueClass,
          eventProcessorName,
      )

  private fun EventOfBusinessTransactionEntity.toDto() =
      EventOfBusinessTransaction(
          creationDate,
          messageDate,
          consumerOffset,
          transactionIdentifier,
          eventKey,
          eventValue,
          eventKeyClass,
          eventValueClass,
          eventProcessorName)
}
