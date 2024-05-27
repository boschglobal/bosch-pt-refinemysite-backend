/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.image.messages.ImageDeletedEventAvro
import com.bosch.pt.csm.cloud.image.messages.ImageScaledEventAvro
import com.bosch.pt.csm.cloud.imagemanagement.image.listener.ImageEventListener
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Listens for image scaling events emitted by the corresponding Azure Function once a newly
 * discovered image Blob has been discovered and scaled successfully.
 */
@Component
@Profile("!test & !restore-db & !kafka-image-listener-disabled")
class ImageScalingListener(
    private val userQueryService: UserQueryService,
    private val imageScalingProcessor: ImageScalingProcessor,
    private val transactionTemplate: TransactionTemplate,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) : ImageEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('image')}"],
      clientIdPrefix = "csm-cloud-user-image")
  override fun listenToImageEvents(
      record: ConsumerRecord<EventMessageKey, SpecificRecordBase?>,
      ack: Acknowledgment
  ) {
    LOGGER.logConsumption(record)

    require(!TransactionSynchronizationManager.isActualTransactionActive()) {
      "No running transaction expected"
    }

    val key = record.key()
    val value = record.value()

    doWithAuthentication {
      executeWithAsyncRequestScope {
        transactionTemplate.executeWithoutResult { handleEvent(key, value) }
      }
    }

    ack.acknowledge()
  }

  private fun handleEvent(key: EventMessageKey, message: SpecificRecordBase?) {
    if (message == null && key is AggregateEventMessageKey) {
      error("Unexpected tombstone message found")
    } else if (message is ImageScaledEventAvro) {
      processImageScaledEvent(message)
    } else if (message is ImageDeletedEventAvro) {
      processImageDeletedEvent(message)
    } else {
      requireNotNull(message) { "Unknown tombstone avro message received: $key" }
      throw IllegalArgumentException("Unknown avro message received: ${message.schema.name}")
    }
  }

  private fun processImageDeletedEvent(imageDeletedEvent: ImageDeletedEventAvro) {
    LOGGER.warn(
        "Image scaled event with id ${imageDeletedEvent.identifier} of file ${imageDeletedEvent.filename} " +
            "was deleted as it couldn't be processed")
    imageScalingProcessor.delete(imageDeletedEvent)
  }

  private fun processImageScaledEvent(imageScaledEvent: ImageScaledEventAvro) {
    imageScalingProcessor.process(imageScaledEvent)
  }

  private fun doWithAuthentication(block: () -> Unit) {
    val systemUser = userQueryService.findOneByIdentifier(systemUserIdentifier.asUserId())
    doWithAuthenticatedUser(systemUser, block)
  }

  companion object {
    private val LOGGER = getLogger(ImageScalingListener::class.java)
  }
}
