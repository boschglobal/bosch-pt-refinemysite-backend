/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKCONSTRAINTCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.service.TaskConstraintProjector
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-task-constraint-projector-listener-disabled")
@Component
class TaskConstraintProjectorEventListener(
    private val logger: Logger,
    private val projector: TaskConstraintProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.task-constraint-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.task-constraint-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.task-constraint-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey &&
        key.aggregateIdentifier.type == TASKCONSTRAINTCUSTOMIZATION.name) {

      when (event) {
        null -> error("Unable to handle task constraint tombstone message.")
        is TaskConstraintCustomizationEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onTaskConstraintEvent(event.aggregate)
              DELETED -> projector.onTaskConstraintDeletedEvent(event.aggregate)
              else -> error("Unhandled task constraint event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
