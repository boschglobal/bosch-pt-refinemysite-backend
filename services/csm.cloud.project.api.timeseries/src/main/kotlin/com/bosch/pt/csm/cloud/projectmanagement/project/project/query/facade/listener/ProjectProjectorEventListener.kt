/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.service.ProjectProjector
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-project-projector-listener-disabled")
@Component
class ProjectProjectorEventListener(
    private val logger: Logger,
    private val projector: ProjectProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.project-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.project-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.project-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == PROJECT.name) {

      when (event) {
        null -> error("Unable to handle project tombstone message.")
        is ProjectEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onProjectEvent(event.aggregate)
              DELETED -> projector.onProjectDeletedEvent(key)
              else -> error("Unhandled project event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
