/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.eventstore

import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.iot.smartsite.project.eventstore.repository.InvitationKafkaEventTestRepository
import com.bosch.pt.iot.smartsite.project.eventstore.repository.ProjectKafkaEventTestRepository
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
open class EventStoreUtilsConfiguration {

  @Bean
  open fun projectEventStoreUtils(
      projectKafkaEventTestRepository: ProjectKafkaEventTestRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(projectKafkaEventTestRepository, mockSchemaRegistryClient)

  @Bean
  open fun invitationEventStoreUtils(
      invitationKafkaEventTestRepository: InvitationKafkaEventTestRepository,
      mockSchemaRegistryClient: SchemaRegistryClient
  ) = EventStoreUtils(invitationKafkaEventTestRepository, mockSchemaRegistryClient)
}
