/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKACTION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.service.TaskConstraintSelectionProjector
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-task-constraint-selection-projector-listener-disabled")
@Component
class TaskConstraintSelectionProjectorEventListener(
    private val logger: Logger,
    private val projector: TaskConstraintSelectionProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.task-constraint-selection-projector.groupId}",
      clientIdPrefix =
          "\${custom.kafka.listener.query.task-constraint-selection-projector.clientIdPrefix}",
      concurrency =
          "\${custom.kafka.listener.query.task-constraint-selection-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == TASKACTION.name) {

      val projectId = key.rootContextIdentifier.asProjectId()

      when (event) {
        null -> error("Unable to handle task constraint selection tombstone message.")
        is TaskActionSelectionEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onTaskConstraintSelectionEvent(event.aggregate, projectId)
              DELETED -> projector.onTaskConstraintSelectionDeletedEvent(event.aggregate)
              else -> error("Unhandled task constraint selection event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
