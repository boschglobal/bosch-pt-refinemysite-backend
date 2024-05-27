/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.facade.listener.online

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.referencedata.craft.event.listener.CraftEventListener
import com.bosch.pt.iot.smartsite.craft.boundary.CraftService
import com.bosch.pt.iot.smartsite.craft.model.Craft.Companion.fromAvroMessage
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Profile("!restore-db & !kafka-craft-listener-disabled")
@Component
open class CraftEventListenerImpl(
    private val craftService: CraftService,
    private val userService: UserService,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UUID,
) : CraftEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('craft')}"],
      clientIdPrefix = "csm-cloud-project-craft")
  override fun listenToCraftEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {

    LOGGER.logConsumption(record)
    val message = record.value()

    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    transactionTemplate.executeWithoutResult { handleEvent(message) }
    ack.acknowledge()
  }

  private fun handleEvent(message: SpecificRecordBase?): Unit =
      if (message is CraftEventAvro) {
        processCraftEvent(message)
      } else {
        throw IllegalArgumentException("Unknown Avro message received: ${message!!.schema.name}")
      }

  private fun processCraftEvent(craftEventAvro: CraftEventAvro): Unit =
      if (craftEventAvro.getName() == CREATED) {
        updateCraft(craftEventAvro.getAggregate())
      } else {
        throw IllegalArgumentException(
            "Unknown craft event type received: ${craftEventAvro.getName()}")
      }

  private fun updateCraft(aggregate: CraftAggregateAvro) {
    val createdBy =
        findUser(aggregate.getAuditingInformation().getCreatedBy().getIdentifier().toUUID())

    val lastModifiedBy =
        findUser(aggregate.getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID())

    craftService.findOneByIdentifier(aggregate.getAggregateIdentifier().getIdentifier().toUUID())
        ?: craftService.create(fromAvroMessage(aggregate, createdBy, lastModifiedBy))
  }

  private fun findUser(identifier: UUID): User =
      userService.findOne(identifier) ?: userService.findOne(systemUserIdentifier)!!

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CraftEventListenerImpl::class.java)
  }
}
