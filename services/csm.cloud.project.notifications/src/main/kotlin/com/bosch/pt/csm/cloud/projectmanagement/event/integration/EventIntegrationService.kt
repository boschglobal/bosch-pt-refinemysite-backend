/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.integration

import com.bosch.pt.csm.cloud.projectmanagement.event.integration.message.Event
import datadog.trace.api.Trace
import java.util.concurrent.ExecutionException
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventIntegrationService(
    private val kafkaTemplate: KafkaTemplate<String, Event>,
    @Value("\${custom.kafka.bindings.event.kafkaTopic}") private val topic: String
) {

  @Trace
  fun send(event: Event) {
    try {
      kafkaTemplate.send(ProducerRecord(topic, event)).get()
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    } catch (e: ExecutionException) {
      throw IllegalStateException(e)
    }
  }
}
