/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.NOTIFICATION
import java.util.concurrent.TimeUnit.DAYS
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
    mongoTemplate.indexOps(NOTIFICATION).apply {
      ensureIndex(Index().on("insertDate", ASC).expire(30, DAYS))
      ensureIndex(Index().on("_id.identifier", ASC))

      // for finding notifications sorted by descending insertDate
      ensureCompoundIndex(
          Document(mapOf("_id.recipientIdentifier" to 1, "merged" to 1, "insertDate" to -1)))

      // for finding mergeable notifications
      ensureCompoundIndex(
          Document(mapOf("_id.recipientIdentifier" to 1, "context.task" to 1, "event.user" to 1)))

      // for marking notifications as merged
      ensureCompoundIndex(
          Document(mapOf("_id.recipientIdentifier" to 1, "externalIdentifier" to 1)))
    }

    mongoTemplate.indexOps(Collections.PROJECT_STATE).apply {
      ensureCompoundIndex(
          Document(mapOf("_id.identifier" to 1, "_id.type" to 1, "_id.version" to 1)))

      // for finding participant(s) by project and user
      ensureCompoundIndex(Document(mapOf("projectIdentifier" to 1, "userIdentifier" to 1)))

      // for finding participants by project and role
      ensureCompoundIndex(Document(mapOf("projectIdentifier" to 1, "role" to 1)))

      // for finding all project participants in ParticipantRepositoryExtensionImpl
      ensureCompoundIndex(Document(mapOf("_class" to 1, "projectIdentifier" to 1)))

      // for finding custom rfv by project and reason
      ensureCompoundIndex(Document(mapOf("_id.type" to 1, "projectIdentifier" to 1, "reason" to 1)))
    }

    mongoTemplate.indexOps(Collections.USER_STATE).apply {
      // for finding users by their external (CIAM) identifier
      ensureIndex(Index().on("externalIdentifier", ASC))
    }
  }
}

private fun IndexOperations.ensureCompoundIndex(keys: Document): String =
    ensureIndex(CompoundIndexDefinition(keys))
