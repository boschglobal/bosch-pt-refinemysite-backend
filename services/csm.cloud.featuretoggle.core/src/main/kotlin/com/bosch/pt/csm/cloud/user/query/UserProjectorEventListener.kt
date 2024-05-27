/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.user.query

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.UPDATED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-user-projector-listener-disabled")
@Component
class UserProjectorEventListener(private val logger: Logger, private val projector: UserProjector) :
    UserEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('user')}"],
      groupId = "\${custom.kafka.listener.user-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.user-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.user-projector.concurrency}")
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey) {
      when (event) {
        null -> projector.onUserDeletedEvent(key)
        is UserEventAvro ->
            when (event.name) {
              CREATED,
              REGISTERED,
              UPDATED -> projector.onUserEvent(event.aggregate)
              else -> logger.info("Unhandled user event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
