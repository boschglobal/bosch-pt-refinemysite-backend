/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.config

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextKafkaEventRepository
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatKafkaEventRepository
import com.bosch.pt.csm.cloud.usermanagement.user.eventstore.repository.UserKafkaEventTestRepository
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class EventStoreUtilsConfiguration {

  @Bean
  fun userEventStoreUtils(
      userKafkaEventTestRepository: UserKafkaEventTestRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(userKafkaEventTestRepository, mockSchemaRegistryClient)

  @Bean
  fun consentsEventStoreUtils(
      consentsContextKafkaEventRepository: ConsentsContextKafkaEventRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(consentsContextKafkaEventRepository, mockSchemaRegistryClient)

  @Bean
  fun patEventStoreUtils(
      patKafkaEventTestRepository: PatKafkaEventRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(patKafkaEventTestRepository, mockSchemaRegistryClient)
}
