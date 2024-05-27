/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREALIST
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.service.WorkAreaListProjector
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.REORDERED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-work-area-list-projector-listener-disabled")
@Component
class WorkAreaListProjectorEventListener(
    private val logger: Logger,
    private val projector: WorkAreaListProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.workarealist-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.workarealist-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.workarealist-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == WORKAREALIST.name) {

      when (event) {
        null -> error("Unable to handle work area list tombstone message.")
        is WorkAreaListEventAvro ->
            when (event.name) {
              CREATED,
              ITEMADDED,
              REORDERED -> projector.onWorkAreaListEvent(event.aggregate)
              else -> error("Unhandled work area list event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
