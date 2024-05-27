/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.service.MilestoneProjector
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-milestone-projector-listener-disabled")
@Component
class MilestoneProjectorEventListener(
    private val logger: Logger,
    private val projector: MilestoneProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.milestone-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.milestone-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.milestone-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == MILESTONE.name) {

      when (event) {
        null -> error("Unable to handle milestone tombstone message.")
        is MilestoneEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onMilestoneEvent(event.aggregate)
              DELETED -> projector.onMilestoneDeletedEvent(event.aggregate)
              else -> error("Unhandled milestone event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
