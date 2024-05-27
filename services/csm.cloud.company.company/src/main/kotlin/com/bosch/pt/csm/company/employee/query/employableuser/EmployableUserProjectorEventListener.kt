/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.companymanagement.company.event.listener.CompanyEventListener
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.listener.UserEventListener
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("!kafka-employable-user-projector-listener-disabled")
@Component
class EmployableUserProjectorEventListener(
    private val projector: EmployableUserProjector,
    private val logger: Logger
) : UserEventListener, CompanyEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('user')}"],
      groupId = "\${custom.kafka.listener.employable-user-projector.groupId}",
      clientIdPrefix =
          "\${custom.kafka.listener.employable-user-projector.userTopic.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.employable-user-projector.userTopic.concurrency}")
  override fun listenToUserEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey) {
      when (event) {
        null ->
            when (key.aggregateIdentifier.type) {
              USER.name -> projector.onUserDeletedEvent(key)
              else -> logger.info("Unhandled tombstone received: $key")
            }
        is UserEventAvro ->
            when (event.name) {
              UserEventEnumAvro.CREATED -> projector.onUserCreatedEvent(event.aggregate)
              UserEventEnumAvro.REGISTERED,
              UserEventEnumAvro.UPDATED -> projector.onUserUpdatedEvent(event.aggregate)
              else -> logger.info("Unhandled user event received: ${event.name}")
            }
      }
    }
    ack.acknowledge()
  }

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('company')}"],
      groupId = "\${custom.kafka.listener.employable-user-projector.groupId}",
      clientIdPrefix =
          "\${custom.kafka.listener.employable-user-projector.companyTopic.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.employable-user-projector.companyTopic.concurrency}")
  override fun listenToCompanyEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    logger.logConsumption(record)

    val key = record.key()
    val event = record.value()

    if (key is AggregateEventMessageKey) {
      when (event) {
        is CompanyEventAvro -> handle(event)
        is EmployeeEventAvro -> handle(event)
      }
    }
    ack.acknowledge()
  }

  @EventListener
  fun handle(event: CompanyEventAvro) {
    when (event.name) {
      CompanyEventEnumAvro.CREATED -> projector.onCompanyCreatedEvent(event.aggregate)
      CompanyEventEnumAvro.UPDATED -> projector.onCompanyUpdatedEvent(event.aggregate)
      CompanyEventEnumAvro.DELETED -> projector.onCompanyDeletedEvent(event.aggregate)
      else -> logger.info("Unhandled company event received: ${event.name}")
    }
  }

  @EventListener
  fun handle(event: EmployeeEventAvro) {
    when (event.name) {
      EmployeeEventEnumAvro.CREATED -> projector.onEmployeeCreatedEvent(event.aggregate)
      EmployeeEventEnumAvro.DELETED -> projector.onEmployeeDeletedEvent(event.aggregate)
      else -> logger.info("Unhandled employee event received: ${event.name}")
    }
  }
}
