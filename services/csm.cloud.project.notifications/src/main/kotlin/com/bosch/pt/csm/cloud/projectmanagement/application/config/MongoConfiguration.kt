/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.bson.UuidRepresentation.JAVA_LEGACY
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MongoConfiguration {

  @Bean
  fun mongoClientSettingsBuilderCustomizer(): MongoClientSettingsBuilderCustomizer =
      MongoClientSettingsBuilderCustomizer {
        it.uuidRepresentation(JAVA_LEGACY)
      }
}
