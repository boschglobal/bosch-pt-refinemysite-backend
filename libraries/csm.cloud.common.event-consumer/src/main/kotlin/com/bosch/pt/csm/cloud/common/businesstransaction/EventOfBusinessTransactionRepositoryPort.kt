/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction

import java.time.LocalDateTime
import java.util.UUID

interface EventOfBusinessTransactionRepositoryPort {

  fun countAllByCreationDateLessThan(creationDate: LocalDateTime): Int

  fun findFirstByEventProcessorNameOrderByOffsetAsc(
      eventProcessorName: String
  ): EventOfBusinessTransaction?

  fun findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
      transactionId: UUID,
      eventProcessorName: String
  ): List<EventOfBusinessTransaction>

  fun removeAllByTransactionIdentifierAndEventProcessorName(
      transactionId: UUID,
      eventProcessorName: String
  )

  fun save(eventOfBusinessTransaction: EventOfBusinessTransaction)

  fun deleteAll()
}
