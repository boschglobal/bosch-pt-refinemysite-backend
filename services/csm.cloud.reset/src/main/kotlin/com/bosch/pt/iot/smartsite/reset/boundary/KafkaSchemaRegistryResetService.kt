/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KafkaSchemaRegistryResetService(
    private val schemaRegistryClient: SchemaRegistryClient,
    @Value("\${stage}") private val stage: String
) {

  fun reset() {
    LOGGER.info("Delete kafka schemas")

    try {
      val subjectsToDelete = schemaRegistryClient.allSubjects.filter { it.contains("csm.$stage.") }
      for (subject in subjectsToDelete) {
        LOGGER.info("Delete schema {}", subject)
        schemaRegistryClient.deleteSubject(subject)
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      throw IllegalStateException(e)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(KafkaSchemaRegistryResetService::class.java)
  }
}
