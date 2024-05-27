/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.service.WorkAreaProjector
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-work-area-projector-listener-disabled")
@Component
class WorkAreaProjectorEventListener(
    private val logger: Logger,
    private val projector: WorkAreaProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.workarea-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.workarea-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.workarea-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == WORKAREA.name) {

      val projectId = key.rootContextIdentifier.asProjectId()

      when (event) {
        null -> error("Unable to handle workarea tombstone message.")
        is WorkAreaEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onWorkAreaEvent(event.aggregate, projectId)
              DELETED -> projector.onWorkAreaDeletedEvent(event.aggregate)
              else -> error("Unhandled work area event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
