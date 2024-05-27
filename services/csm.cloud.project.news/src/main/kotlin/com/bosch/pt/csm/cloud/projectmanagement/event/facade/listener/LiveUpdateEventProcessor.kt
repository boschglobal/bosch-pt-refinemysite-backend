/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.AbstractEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.event.model.Event
import com.bosch.pt.csm.cloud.projectmanagement.event.model.LiveUpdateEvent
import com.bosch.pt.csm.cloud.projectmanagement.event.model.ObjectIdentifierWithVersion
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.Trace
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ExecutionException
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * This class converts a project event into a live-update event that is forwarded to the event
 * service to be distributed to the connected clients.
 */
@Component
class LiveUpdateEventProcessor(
    private val participantMappingRepository: ParticipantMappingRepository,
    private val kafkaTemplate: KafkaTemplate<String, Event>,
    private val objectMapper: ObjectMapper,
    @Value("\${custom.kafka.bindings.event.kafkaTopic}") private val topic: String
) : AbstractEventProcessor() {

  @Trace(operationName = "send live update for project events")
  override fun process(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      recordTimestamp: LocalDateTime
  ) {
    if (!shouldProcessStateOnly(recordTimestamp)) {
      sendProjectUpdate(key, value)
    }
  }

  @Trace(operationName = "send live update for user events")
  fun processUserEvents(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      recordTimestamp: LocalDateTime
  ) {
    if (!shouldProcessStateOnly(recordTimestamp)) {
      sendUserUpdate(key, value)
    }
  }

  private fun sendUserUpdate(key: EventMessageKey, value: SpecificRecordBase?) {
    // so far, only aggregate events need to be sent via live updates
    if (key !is AggregateEventMessageKey) return

    val receivers = setOf(key.rootContextIdentifier)

    val root = ObjectIdentifier("USER", key.rootContextIdentifier)
    send(key, value, root, receivers)
  }

  private fun sendProjectUpdate(key: EventMessageKey, value: SpecificRecordBase?) {
    // so far, only aggregate events need to be sent via live updates
    if (key !is AggregateEventMessageKey) return

    val projectParticipants =
        participantMappingRepository.findAllByProjectIdentifier(key.rootContextIdentifier)

    val receivers = projectParticipants.map { it.userIdentifier }.toSet()

    val root = ObjectIdentifier("PROJECT", key.rootContextIdentifier)
    send(key, value, root, receivers)
  }

  @Suppress("ThrowsCount")
  private fun send(
      key: AggregateEventMessageKey,
      value: SpecificRecordBase?,
      root: ObjectIdentifier,
      receivers: Set<UUID>
  ) {
    val objectIdentifier =
        ObjectIdentifierWithVersion(
            key.aggregateIdentifier.type,
            getIdentifier(value, key).toUUID(),
            key.aggregateIdentifier.version)

    val liveUpdateEvent = LiveUpdateEvent(root, objectIdentifier, value?.get("name").toString())

    try {
      val jsonMessage = objectMapper.writer().writeValueAsString(liveUpdateEvent)
      val event = Event(receivers, "update", jsonMessage)
      val producerRecord = ProducerRecord(topic, key.rootContextIdentifier.toString(), event)
      kafkaTemplate.send(producerRecord).get()
    } catch (e: JsonProcessingException) {
      throw IllegalStateException(e)
    } catch (e: InterruptedException) {
      throw IllegalStateException(e)
    } catch (e: ExecutionException) {
      throw IllegalStateException(e)
    }
  }

  // Task actions get the UUID of the task, since it is a 1:1 relation and the REST API
  // contains the UUID of the corresponding task to build a unique url for the task actions
  private fun getIdentifier(
      value: SpecificRecordBase?,
      messageKey: AggregateEventMessageKey
  ): String =
      if (value is TaskActionSelectionEventAvro) {
        value.aggregate.task.identifier
      } else {
        messageKey.aggregateIdentifier.identifier.toString()
      }
}
