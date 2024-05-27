/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.application.config.properties.MultipleMongoDbProperties
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import java.util.Objects
import org.bson.UuidRepresentation
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory

@Configuration
@EnableConfigurationProperties(MultipleMongoDbProperties::class)
class MongoDbConfiguration(private val dbProperties: MultipleMongoDbProperties) {

  @Bean(name = ["activityServiceMongoTemplate"])
  fun activityServiceMongoTemplate(): MongoTemplate =
      getMongoTemplate(dbProperties.activityService.uri, UuidRepresentation.STANDARD)

  @Bean(name = ["jobServiceMongoTemplate"])
  fun jobServiceMongoTemplate(): MongoTemplate =
      getMongoTemplate(dbProperties.jobService.uri, UuidRepresentation.STANDARD)

  @Bean(name = ["projectApiTimeseriesServiceMongoTemplate"])
  fun projectApiTimeseriesServiceMongoTemplate(): MongoTemplate =
      getMongoTemplate(dbProperties.projectApiTimeseriesService.uri, UuidRepresentation.STANDARD)

  @Bean(name = ["projectServiceMongoTemplate"])
  fun projectServiceMongoTemplate(): MongoTemplate =
      getMongoTemplate(dbProperties.projectService.uri, UuidRepresentation.STANDARD)

  @Bean(name = ["notificationServiceMongoTemplate"])
  fun notificationServiceMongoTemplate(): MongoTemplate =
      getMongoTemplate(dbProperties.notificationService.uri, UuidRepresentation.STANDARD)

  private fun getMongoTemplate(uri: String, uuidRepresentation: UuidRepresentation): MongoTemplate =
      MongoClientSettings.builder()
          .applyConnectionString(ConnectionString(uri))
          .uuidRepresentation(uuidRepresentation)
          .build()
          .let {
            MongoTemplate(
                SimpleMongoClientDatabaseFactory(
                    MongoClients.create(it),
                    Objects.requireNonNull(ConnectionString(uri).database)))
          }
}
