/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.event.integration

import com.bosch.pt.csm.cloud.common.kafka.logProduction
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventService(
    @Qualifier("json") private val kafkaTemplate: KafkaTemplate<String, Event>,
    private val logger: Logger,
    @Value("\${custom.kafka.bindings.event.kafkaTopic}") private val topic: String
) {
  fun send(userIdentifier: UserIdentifier, message: Any) {
    val event = Event(setOf(userIdentifier.value), "job", message)
    kafkaTemplate.send(ProducerRecord(topic, event)).get().also {
      logger.logProduction(it.producerRecord)
    }
  }
}

data class Event(val receivers: Set<String>, val eventType: String, val message: Any)
