/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.mongodb

import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.mongodb.repository.MongoRepository

interface MongoEventOfBusinessTransactionRepository :
    MongoRepository<EventsOfBusinessTransactionDocument, UUID> {

  fun countAllByCreationDateLessThan(creationDate: LocalDateTime): Int

  fun findFirstByEventProcessorNameOrderByOffsetAsc(
      eventProcessorName: String
  ): EventsOfBusinessTransactionDocument?

  fun findAllByTransactionIdentifierAndEventProcessorNameOrderByOffsetAsc(
      transactionIdentifier: UUID,
      eventProcessorName: String
  ): List<EventsOfBusinessTransactionDocument>

  fun removeAllByTransactionIdentifierAndEventProcessorName(
      transactionIdentifier: UUID,
      eventProcessorName: String
  )
}
