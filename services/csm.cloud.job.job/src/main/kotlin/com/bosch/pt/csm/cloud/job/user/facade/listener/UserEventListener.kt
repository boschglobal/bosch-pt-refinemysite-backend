/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.facade.listener

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.ExternalUserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.UserChangedEvent
import com.bosch.pt.csm.cloud.job.user.query.UserDeletedEvent
import com.bosch.pt.csm.cloud.job.user.query.UserProjector
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecord
import org.apache.commons.lang3.LocaleUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class UserEventListener(private val userProjector: UserProjector, private val logger: Logger) {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.user.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.user.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.user.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.user.concurrency}",
      containerFactory = "nonTransactionalKafkaListenerContainerFactory")
  fun listen(record: ConsumerRecord<EventMessageKey, SpecificRecord?>) {
    logger.logConsumption(record)
    val key = record.key()
    if (key is AggregateEventMessageKey) handleEvent(key, record.value())
    else logger.debug("Dropping event with unknown key type: $key")
  }

  private fun handleEvent(key: AggregateEventMessageKey, event: SpecificRecord?) {
    when (event) {
      null -> handleTombstoneEvent(key)
      is UserEventAvro -> handleUserEvent(key, event)
      else -> logger.debug("Dropping unknown event: ${event.schema.name}")
    }
  }

  private fun handleTombstoneEvent(key: AggregateEventMessageKey) {
    if (UsermanagementAggregateTypeEnum.USER.value == key.aggregateIdentifier.type) {
      val userIdentifier = UserIdentifier(key.aggregateIdentifier.identifier.toString())
      userProjector.handle(UserDeletedEvent(userIdentifier))
    } else {
      logger.debug("Dropping unknown tombstone message: ${key.aggregateIdentifier}")
    }
  }

  private fun handleUserEvent(key: AggregateEventMessageKey, userEventAvro: UserEventAvro) {
    val userIdentifier = UserIdentifier(key.aggregateIdentifier.identifier.toString())
    val externalUserIdentifier = ExternalUserIdentifier(userEventAvro.getAggregate().getUserId())
    val locale = LocaleUtils.toLocale(userEventAvro.getAggregate().getLocale())
    userProjector.handle(UserChangedEvent(userIdentifier, externalUserIdentifier, locale))
  }
}
