/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.common.uuid.UuidIdentifiableToUuidConverter
import org.bson.UuidRepresentation.STANDARD
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
open class MongoConfiguration {

  @Bean
  open fun mongoClientSettingsBuilderCustomizer(): MongoClientSettingsBuilderCustomizer =
      MongoClientSettingsBuilderCustomizer {
        it.uuidRepresentation(STANDARD)
      }

  // JSR-303 validation
  @Bean open fun localValidatorFactoryBean() = LocalValidatorFactoryBean()

  // JSR-303 validation
  @Bean
  open fun validatingMongoEventListener(localValidatorFactoryBean: LocalValidatorFactoryBean) =
      ValidatingMongoEventListener(localValidatorFactoryBean)

  @Bean
  open fun mongoCustomConversions() =
      MongoCustomConversions(listOf(UuidIdentifiableToUuidConverter()))
}
