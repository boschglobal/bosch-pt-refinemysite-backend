/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
open class MockSchemaRegistryClientConfiguration {

  @Bean open fun mockSchemaRegistryClient() = MockSchemaRegistryClient()
}
