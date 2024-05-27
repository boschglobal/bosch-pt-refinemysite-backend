/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatProjector
import com.bosch.pt.csm.cloud.usermanagement.pat.common.PatAggregateTypeEnum.PAT
import com.bosch.pt.csm.cloud.usermanagement.pat.event.listener.PatEventListener
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-pat-projector-listener-disabled")
@Component
class PatProjectorEventListener(private val logger: Logger, private val projector: PatProjector) :
    PatEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.pat.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.pat-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.pat-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.pat-projector.concurrency}")
  override fun listenToPatEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == PAT.name) {
      when (event) {
        null -> projector.onPatDeletedEvent(key)
        is PatCreatedEventAvro -> projector.onPatCreatedEvent(event)
        is PatUpdatedEventAvro -> projector.onPatUpdatedEvent(event)
      }
    }

    ack.acknowledge()
  }
}
