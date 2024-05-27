/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.jpa

import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface JpaEventOfBusinessTransactionRepository :
    JpaRepository<EventOfBusinessTransactionEntity, Long> {

  fun countAllByCreationDateLessThan(creationDate: LocalDateTime): Int

  fun findFirstByEventProcessorNameOrderByConsumerOffsetAsc(
      eventProcessorName: String
  ): EventOfBusinessTransactionEntity?

  fun findAllByTransactionIdentifierAndEventProcessorName(
      transactionIdentifier: UUID,
      eventProcessorName: String
  ): Collection<EventOfBusinessTransactionEntity>

  fun removeAllByTransactionIdentifierAndEventProcessorName(
      transactionIdentifier: UUID,
      eventProcessorName: String
  )
}
