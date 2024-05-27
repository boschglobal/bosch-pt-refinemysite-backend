/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.application.config

import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshot
import com.bosch.pt.csm.cloud.job.job.query.JobProjection
import java.time.Duration
import java.util.concurrent.TimeUnit.DAYS
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class MongoIndexConfiguration(
    private val mongoTemplate: MongoTemplate,
    @Value("\${custom.job.ttl.days}") private val ttlDays: Long,
    private val logger: Logger
) {

  @EventListener(ApplicationReadyEvent::class)
  fun initIndicesAfterStartup() {
    logger.info("Ensuring TTL for Jobs is set to $ttlDays days.")
    configureExpiration(JobSnapshot.COLLECTION_NAME)
    configureExpiration(JobProjection.COLLECTION_NAME)
  }

  private fun configureExpiration(collectionName: String, fieldName: String = "lastModifiedDate") {
    val indexName = "${fieldName}_TTL"
    mongoTemplate.indexOps(collectionName).apply {
      indexInfo.firstOrNull { it.name == indexName }?.let {
        if (it.expireAfter.isPresent && it.expireAfter.get() != Duration.ofDays(ttlDays)) {
          logger.info(
              "Index definition for collection $collectionName on $fieldName changed, will recreate.")
          dropIndex(indexName)
        }
      }
      ensureIndex(Index().on(fieldName, ASC).named(indexName).expire(ttlDays, DAYS))
    }
  }
}
