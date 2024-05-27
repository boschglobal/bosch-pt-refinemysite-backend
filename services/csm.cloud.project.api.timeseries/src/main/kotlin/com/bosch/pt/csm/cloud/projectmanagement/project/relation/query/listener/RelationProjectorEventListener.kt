/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RELATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service.RelationProjector
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.UNCRITICAL
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-relation-projector-listener-disabled")
@Component
class RelationProjectorEventListener(
    private val logger: Logger,
    private val projector: RelationProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.relation-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.relation-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.relation-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == RELATION.name) {

      when (event) {
        null -> error("Unable to handle relation tombstone message.")
        is RelationEventAvro ->
            when (event.name) {
              CREATED,
              CRITICAL,
              UNCRITICAL -> projector.onRelationEvent(event.aggregate)
              DELETED -> projector.onRelationDeletedEvent(event.aggregate)
              else -> error("Unhandled relation event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
