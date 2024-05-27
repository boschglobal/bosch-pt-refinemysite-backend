/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.mongodb

import com.bosch.pt.csm.cloud.common.businesstransaction.mongodb.EventsOfBusinessTransactionDocument.Companion.COLLECTION_NAME
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = COLLECTION_NAME)
@TypeAlias("EventsOfBusinessTransactionDocumentV2")
class EventsOfBusinessTransactionDocument(
    @Id var identifier: UUID,
    var creationDate: LocalDateTime,
    var messageDate: LocalDateTime,
    var offset: Long,
    var transactionIdentifier: UUID,
    var eventProcessorName: String,
    var event: Event
) {
  companion object {
    const val COLLECTION_NAME = "EventsOfBusinessTransaction"
  }
}

@TypeAlias("EventV2")
class Event(
    var eventKey: ByteArray,
    var eventValue: ByteArray,
    var eventKeyClass: String,
    var eventValueClass: String,
)
