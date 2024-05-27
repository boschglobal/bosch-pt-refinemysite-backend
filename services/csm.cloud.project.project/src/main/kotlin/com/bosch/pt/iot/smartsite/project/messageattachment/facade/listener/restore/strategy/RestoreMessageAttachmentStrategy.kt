/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGEATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
class RestoreMessageAttachmentStrategy(
    private val messageRepository: MessageRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, messageAttachmentRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      MESSAGEATTACHMENT.value == record.key().aggregateIdentifier.type &&
          record.value() is MessageAttachmentEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val event = record.value() as MessageAttachmentEventAvro?
    assertEventNotNull(event, record.key())

    if (event!!.name == CREATED || event.name == UPDATED) {
      val aggregate = event.aggregate
      val messageAttachment = findMessageAttachment(aggregate.aggregateIdentifier)
      if (messageAttachment == null) {
        createMessageAttachment(aggregate)
      } else {
        updateMessageAttachment(messageAttachment, aggregate)
      }
    } else {
      handleInvalidEventType(event.name.name)
    }
  }

  private fun createMessageAttachment(aggregate: MessageAttachmentAggregateAvro) {
    entityManager.persist(
        MessageAttachment().apply {
          setMessageAttachmentAttributes(this, aggregate)
          setAuditAttributes(this, aggregate.auditingInformation)
        })
  }

  private fun updateMessageAttachment(
      messageAttachment: MessageAttachment,
      aggregate: MessageAttachmentAggregateAvro
  ) {
    update(
        messageAttachment,
        object : DetachedEntityUpdateCallback<MessageAttachment> {
          override fun update(entity: MessageAttachment) {
            setMessageAttachmentAttributes(entity, aggregate)
            setAuditAttributes(entity, aggregate.auditingInformation)
          }
        })
  }

  private fun setMessageAttachmentAttributes(
      messageAttachment: MessageAttachment,
      aggregate: MessageAttachmentAggregateAvro
  ) {
    val attachmentAvro = aggregate.attachment
    messageAttachment.identifier = UUID.fromString(aggregate.aggregateIdentifier.identifier)
    messageAttachment.version = aggregate.aggregateIdentifier.version
    val message = findMessageOrFail(aggregate.message)
    messageAttachment.message = message
    messageAttachment.topic = message.topic
    messageAttachment.task = message.topic.task
    messageAttachment.fileName = attachmentAvro.fileName
    messageAttachment.fileSize = attachmentAvro.fileSize
    messageAttachment.imageHeight = attachmentAvro.height
    messageAttachment.imageWidth = attachmentAvro.width
    messageAttachment.setFullAvailable(attachmentAvro.fullAvailable)
    messageAttachment.setSmallAvailable(attachmentAvro.smallAvailable)
    messageAttachment.captureDate =
        if (attachmentAvro.captureDate == null) null else Date(attachmentAvro.captureDate)
  }

  private fun findMessageAttachment(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ): MessageAttachment? =
      messageAttachmentRepository.findOneWithDetailsByIdentifier(
          UUID.fromString(aggregateIdentifierAvro.identifier))

  private fun findMessageOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Message =
      requireNotNull(
          messageRepository.findOneWithDetailsByIdentifier(
              aggregateIdentifierAvro.identifier.asMessageId())) {
            "Message missing: ${aggregateIdentifierAvro.identifier}"
          }
}
