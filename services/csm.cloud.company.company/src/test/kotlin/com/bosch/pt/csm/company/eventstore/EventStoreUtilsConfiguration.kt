/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class EventStoreUtilsConfiguration {

  @Bean
  fun companyEventStoreUtils(
      companyKafkaEventTestRepository: CompanyKafkaEventTestRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(companyKafkaEventTestRepository, mockSchemaRegistryClient)
}
