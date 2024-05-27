/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key.TOPIC_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import datadog.trace.api.Trace
import java.util.TimeZone
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
class TopicAttachmentService(
    private val idGenerator: IdGenerator,
    private val topicRepository: TopicRepository,
    private val topicAttachmentRepository: TopicAttachmentRepository,
    private val attachmentService: AttachmentService
) {

  @Trace
  @Transactional
  @NoPreAuthorize
  fun updateImageMetadata(
      attachmentIdentifier: UUID,
      originalFIleSize: Long,
      imageMetadata: ImageMetadataDto?
  ) =
      attachmentService.updateImageMetadata(
          attachmentIdentifier, originalFIleSize, imageMetadata, UPDATED)

  @Trace
  @Transactional
  @PreAuthorize(
      "@topicAuthorizationComponent.hasCreateAttachmentPermissionOnTopic(#topicIdentifier)")
  fun saveTopicAttachment(
      binaryData: ByteArray,
      topicIdentifier: TopicId,
      fileName: String,
      attachmentIdentifier: UUID?,
      timeZone: TimeZone
  ): UUID {

    val topic =
        topicRepository.findOneByIdentifier(topicIdentifier)
            ?: throw AggregateNotFoundException(
                TOPIC_VALIDATION_ERROR_NOT_FOUND, topicIdentifier.toString())

    // Generate attachment identifier if none is given
    var topicAttachment = createTopicAttachment(fileName, binaryData.size.toLong(), topic)
    topicAttachment.identifier = attachmentIdentifier ?: idGenerator.generateId()
    topicAttachment = topicAttachmentRepository.save(topicAttachment, CREATED)

    attachmentService.storeBlob(
        binaryData, topicAttachment, BlobMetadata.from(fileName, timeZone, topicAttachment))

    return topicAttachment.identifier!!
  }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  fun deletePartitioned(topicIds: List<Long>) {
    val attachments = topicAttachmentRepository.getByTopicIdsPartitioned(topicIds)
    attachments.forEach { blobOwner: TopicAttachment ->
      attachmentService.deleteAllResolutionsOfImageIfExists(blobOwner)
    }
    topicAttachmentRepository.deletePartitioned(attachments.map { it.id!! })
  }

  private fun createTopicAttachment(
      fileName: String,
      fileSize: Long,
      topic: Topic,
  ) = TopicAttachment(null, fileName, fileSize, 0, 0, topic.task, topic)
}
