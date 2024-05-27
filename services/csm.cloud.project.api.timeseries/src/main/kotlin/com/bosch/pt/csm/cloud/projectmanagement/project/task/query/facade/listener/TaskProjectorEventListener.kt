/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.service.TaskProjector
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service.TaskScheduleProjector
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.CREATED as SCHEDULE_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED as SCHEDULE_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED as SCHEDULE_UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * This event listener is responsible for both, task and task schedule events so that both types of
 * events are received strictly in order. This way, the [TaskScheduleProjector] can reliably
 * determine the corresponding task version for each task schedule event. This is important because
 * the tasks REST endpoint hides the existence of schedules such that a changed schedule translates
 * to a task with a changed start/end date.
 */
@Profile("!kafka-task-projector-listener-disabled")
@Component
class TaskProjectorEventListener(
    private val logger: Logger,
    private val taskProjector: TaskProjector,
    private val scheduleProjector: TaskScheduleProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.task-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.task-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.task-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == TASK.name) {

      when (event) {
        null -> error("Unable to handle task tombstone message.")
        is TaskEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED,
              ASSIGNED,
              UNASSIGNED,
              CLOSED,
              STARTED,
              SENT,
              RESET,
              ACCEPTED -> taskProjector.onTaskEvent(event.aggregate)
              DELETED -> taskProjector.onTaskDeletedEvent(event.aggregate)
              else -> error("Unhandled task event received: ${event.name}")
            }
      }
    } else if (key is AggregateEventMessageKey &&
        key.aggregateIdentifier.type == TASKSCHEDULE.name) {

      val projectId = key.rootContextIdentifier.asProjectId()

      when (event) {
        null -> error("Unable to handle task schedule tombstone message.")
        is TaskScheduleEventAvro ->
            when (event.name) {
              SCHEDULE_CREATED,
              SCHEDULE_UPDATED -> scheduleProjector.onTaskScheduleEvent(event.aggregate, projectId)
              SCHEDULE_DELETED -> scheduleProjector.onTaskScheduleDeletedEvent(event.aggregate)
              else -> error("Unhandled task schedule event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
