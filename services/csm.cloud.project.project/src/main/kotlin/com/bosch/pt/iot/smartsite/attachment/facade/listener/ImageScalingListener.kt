/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.image.messages.ImageDeletedEventAvro
import com.bosch.pt.csm.cloud.image.messages.ImageScaledEventAvro
import com.bosch.pt.csm.cloud.imagemanagement.image.listener.ImageEventListener
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.user.boundary.UserService
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

/**
 * Listens for image scaling events emitted by the corresponding csm.cloud.image.scale once a newly
 * discovered image Blob has been discovered and scaled successfully.
 */
@Component
@Profile("!test & !restore-db & & !kafka-image-listener-disabled")
open class ImageScalingListener(
    private val imageDeletingProcessor: ImageDeletingProcessor,
    private val imageScalingProcessor: ImageScalingProcessor,
    private val transactionTemplate: TransactionTemplate,
    private val userQueryService: UserService,
    @param:Value("\${system.user.identifier}") private val systemUserIdentifier: UUID
) : ImageEventListener {

  @Trace
  @KafkaListener(
      topics = ["#{kafkaTopicProperties.getTopicForChannel('image')}"],
      clientIdPrefix = "csm-cloud-project-image")
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

  private fun handleEvent(key: EventMessageKey, message: SpecificRecordBase?) =
      if (message == null && key is AggregateEventMessageKey) {
        error("Unexpected tombstone message found")
      } else if (message is ImageScaledEventAvro) {
        processImageScaledEvent(message)
      } else if (message is ImageDeletedEventAvro) {
        processImageDeletedEvent(message)
      } else {
        requireNotNull(message) { "Unknown tombstone avro message received: $key" }
        throw IllegalArgumentException("Unknown Avro message received: ${message.schema.name}")
      }

  private fun processImageDeletedEvent(imageDeletedEvent: ImageDeletedEventAvro) =
      try {
        LOGGER.warn(
            "Image scaled event with id ${imageDeletedEvent.identifier} of file ${imageDeletedEvent.filename} " +
                "was deleted as it couldn't be processed")
        imageDeletingProcessor.process(imageDeletedEvent)
      } catch (e: BlockOperationsException) {
        LOGGER.warn(
            "Image deleted event with id ${imageDeletedEvent.identifier} couldn't be processed " +
                "because modifying operations are blocked due to maintenance.")
        throw e
      }

  private fun processImageScaledEvent(imageScaledEvent: ImageScaledEventAvro) =
      try {
        imageScalingProcessor.process(imageScaledEvent)
      } catch (e: BlockOperationsException) {
        LOGGER.warn(
            "Image scaled event with id ${imageScaledEvent.identifier} couldn't be processed " +
                "because modifying operations are blocked due to maintenance.")
        throw e
      }

  private fun doWithAuthentication(block: () -> Unit) =
      doWithAuthenticatedUser(userQueryService.findOneByIdentifier(systemUserIdentifier), block)

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ImageScalingListener::class.java)
  }
}
