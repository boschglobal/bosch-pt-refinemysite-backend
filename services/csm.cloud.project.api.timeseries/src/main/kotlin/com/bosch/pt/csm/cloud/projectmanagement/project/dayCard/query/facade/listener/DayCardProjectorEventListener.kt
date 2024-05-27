/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.service.DayCardProjector
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.COMPLETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-day-card-projector-listener-disabled")
@Component
class DayCardProjectorEventListener(
    private val logger: Logger,
    private val projector: DayCardProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.daycard-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.daycard-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.daycard-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == DAYCARD.name) {

      val projectId = key.rootContextIdentifier.asProjectId()

      when (event) {
        null -> error("Unable to handle day card tombstone message.")
        is DayCardEventG2Avro ->
            when (event.name) {
              CREATED,
              UPDATED,
              CANCELLED,
              COMPLETED,
              RESET,
              APPROVED -> projector.onDayCardEvent(event.aggregate, projectId)
              DELETED -> projector.onDayCardDeletedEvent(event.aggregate)
              else -> error("Unhandled day card event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
