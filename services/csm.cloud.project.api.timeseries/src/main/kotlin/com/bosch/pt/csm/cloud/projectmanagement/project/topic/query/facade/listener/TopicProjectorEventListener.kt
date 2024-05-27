/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.service.TopicProjector
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-topic-projector-listener-disabled")
@Component
class TopicProjectorEventListener(
    private val logger: Logger,
    private val projector: TopicProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.topic-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.topic-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.topic-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == TOPIC.name) {

      val projectId = key.rootContextIdentifier.asProjectId()

      when (event) {
        null -> error("Unable to handle topic tombstone message.")
        is TopicEventG2Avro ->
            when (event.name) {
              CREATED,
              UPDATED,
              ESCALATED,
              DEESCALATED -> projector.onTopicEvent(event.aggregate, projectId)
              DELETED -> projector.onTopicDeletedEvent(event.aggregate)
              else -> error("Unhandled topic event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
