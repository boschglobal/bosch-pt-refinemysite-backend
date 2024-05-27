/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.ACTIVITY
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.USER_STATE
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
    mongoTemplate.indexOps(ACTIVITY).apply {
      ensureIndex(Index().on("attachment.identifier", ASC).sparse())
      ensureIndex(Index().on("identifier", ASC))

      ensureCompoundIndex(Document(mapOf("context.task" to 1, "event.date" to -1)))
      ensureCompoundIndex(Document(mapOf("event.date" to -1, "_id.type" to 1, "_id.version" to 1)))
    }

    mongoTemplate.indexOps(USER_STATE).apply { ensureIndex(Index().on("externalIdentifier", ASC)) }

    mongoTemplate.indexOps(PROJECT_STATE).apply {
      ensureIndex(Index().on("_id.identifier", ASC))
      ensureCompoundIndex(
          Document(mapOf("_id.type" to 1, "_id.identifier" to 1, "projectIdentifier" to 1)))

      // for participants
      ensureCompoundIndex(Document(mapOf("projectIdentifier" to 1, "userIdentifier" to 1)))

      ensureCompoundIndex(Document(mapOf("projectIdentifier" to 1, "taskIdentifier" to 1)))
    }
  }

  private fun IndexOperations.ensureCompoundIndex(keys: Document): String =
      ensureIndex(CompoundIndexDefinition(keys))
}
