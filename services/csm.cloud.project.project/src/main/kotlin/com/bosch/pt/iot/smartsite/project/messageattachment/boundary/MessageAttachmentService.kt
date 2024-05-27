/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key.MESSAGE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import datadog.trace.api.Trace
import java.util.TimeZone
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
class MessageAttachmentService(
    private val idGenerator: IdGenerator,
    private val messageRepository: MessageRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val attachmentService: AttachmentService
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  fun updateImageMetadata(
      attachmentIdentifier: UUID,
      fileSize: Long,
      imageMetadata: ImageMetadataDto?
  ) = attachmentService.updateImageMetadata(attachmentIdentifier, fileSize, imageMetadata, UPDATED)

  @Trace
  @Transactional
  @PreAuthorize(
      "@messageAuthorizationComponent.hasCreateAttachmentPermissionOnMessage(#messageIdentifier.identifier)")
  fun saveMessageAttachment(
      binaryData: ByteArray,
      messageIdentifier: MessageId,
      fileName: String,
      attachmentIdentifier: UUID?,
      timeZone: TimeZone
  ): UUID {
    val message =
        messageRepository.findOneByIdentifier(messageIdentifier)
            ?: throw AggregateNotFoundException(
                MESSAGE_VALIDATION_ERROR_NOT_FOUND, messageIdentifier.toString())

    // Generate attachment identifier if none is given
    var messageAttachment = createMessageAttachment(fileName, binaryData.size.toLong(), message)
    messageAttachment.identifier = attachmentIdentifier ?: idGenerator.generateId()
    messageAttachment = messageAttachmentRepository.save(messageAttachment, CREATED)
    attachmentService.storeBlob(
        binaryData, messageAttachment, BlobMetadata.from(fileName, timeZone, messageAttachment))
    return messageAttachment.identifier!!
  }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  fun deletePartitioned(messageIds: List<Long>) {
    val attachments = messageAttachmentRepository.getByMessageIdsPartitioned(messageIds)
    attachments.forEach { blobOwner: MessageAttachment? ->
      attachmentService.deleteAllResolutionsOfImageIfExists(blobOwner!!)
    }
    messageAttachmentRepository.deletePartitioned(attachments.map { it.id!! })
  }

  private fun createMessageAttachment(
      fileName: String,
      fileSize: Long,
      message: Message,
  ) = MessageAttachment(null, fileName, fileSize, 0, 0, message.topic.task, message.topic, message)
}
