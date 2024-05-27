/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application.config

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.featuretoggle.eventstore.FeaturetoggleKafkaEventTestRepository
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class EventStoreUtilsConfiguration {

  @Bean
  fun featuretoggleEventStoreUtils(
      featuretoggleKafkaEventTestRepository: FeaturetoggleKafkaEventTestRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(featuretoggleKafkaEventTestRepository, mockSchemaRegistryClient)
}
