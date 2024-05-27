/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKDAYCONFIGURATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.ProjectEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.service.WorkDayConfigurationProjector
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-work-day-config-projector-listener-disabled")
@Component
class WorkDayConfigurationProjectorEventListener(
    private val logger: Logger,
    private val projector: WorkDayConfigurationProjector
) : ProjectEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.project.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.workday-config-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.workday-config-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.workday-config-projector.concurrency}")
  override fun listenToProjectEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey &&
        key.aggregateIdentifier.type == WORKDAYCONFIGURATION.name) {

      when (event) {
        null -> error("Unable to handle workday configuration tombstone message.")
        is WorkdayConfigurationEventAvro ->
            when (event.name) {
              CREATED,
              UPDATED -> projector.onWorkDayConfigurationEvent(event.aggregate)
              DELETED -> projector.onWorkDayConfigurationDeletedEvent(event.aggregate)
              else -> error("Unhandled work day configuration event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
