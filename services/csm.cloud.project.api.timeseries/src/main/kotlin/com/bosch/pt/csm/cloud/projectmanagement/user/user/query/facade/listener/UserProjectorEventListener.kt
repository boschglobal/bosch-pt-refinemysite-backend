/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatProjector
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service.UserProjector
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.DELETED
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
class UserProjectorEventListener(
    private val logger: Logger,
    private val projector: UserProjector,
    private val patProjector: PatProjector
) : UserEventListener {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.user.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.query.user-projector.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.query.user-projector.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.query.user-projector.concurrency}")
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey && key.aggregateIdentifier.type == USER.name) {
      when (event) {
        null -> projector.onUserDeletedEvent(key)
        is UserEventAvro ->
            when (event.name) {
              CREATED,
              REGISTERED,
              UPDATED -> {
                projector.onUserEvent(event.aggregate)
                val userId = event.aggregate.aggregateIdentifier.identifier.toUUID().asUserId()
                if (event.aggregate.locked) {
                  patProjector.onUserLockedEvent(userId)
                } else {
                  patProjector.onUserUnlockedEvent(userId)
                }
              }
              // There shouldn't be deleted events anymore in the event stream,
              // but we keep the logic for safety reasons
              DELETED -> projector.onUserDeletedEvent(key)
              else -> error("Unhandled user event received: ${event.name}")
            }
      }
    }

    ack.acknowledge()
  }
}
