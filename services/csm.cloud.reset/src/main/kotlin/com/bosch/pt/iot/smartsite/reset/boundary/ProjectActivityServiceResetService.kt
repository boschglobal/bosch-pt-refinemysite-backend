/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.bosch.pt.iot.smartsite.reset.Resettable
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service

@Service
class ProjectActivityServiceResetService(
    @Qualifier("activityServiceMongoTemplate") private val mongoTemplate: MongoTemplate
) : Resettable {

  override fun reset() {
    LOGGER.info("Reset activity-service database ...")
    mongoTemplate.collectionNames.forEach {
      mongoTemplate.getCollection(it).deleteMany(BsonDocument.parse("{}"))
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProjectActivityServiceResetService::class.java)
  }
}
