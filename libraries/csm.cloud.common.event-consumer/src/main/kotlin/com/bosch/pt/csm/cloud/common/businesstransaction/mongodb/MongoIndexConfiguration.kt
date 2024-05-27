/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.mongodb

import org.bson.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexOperations

@Configuration
class MongoIndexConfiguration(private val mongoTemplate: MongoTemplate) {

  @EventListener(ApplicationReadyEvent::class)
  fun initIndicesAfterStartup() {
    mongoTemplate.indexOps(EventsOfBusinessTransactionDocument.COLLECTION_NAME).apply {
      ensureIndex(Index().on("identifier", ASC))
      ensureCompoundIndex(
          Document(
              mapOf("transactionIdentifier" to 1, "eventProcessorName" to 1, "creationDate" to 1)))
    }
  }

  private fun IndexOperations.ensureCompoundIndex(keys: Document): String =
      ensureIndex(CompoundIndexDefinition(keys))
}
