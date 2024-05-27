/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.authorizedclient

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@Configuration
class MongoConfig {

  /**
   * Registers a [MappingMongoConverter] that replaces dot-notations with custom '___' character
   * sequence. This is needed as KEYCLOAK1 JWT contains properties with dots which are subject to
   * interpretation in MongoDB documents.
   */
  @Bean
  fun customMappingMongoConverter(
      context: MongoMappingContext,
      conversions: MongoCustomConversions
  ) =
      MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context).apply {
        customConversions = conversions
        setMapKeyDotReplacement("___")
      }
}
