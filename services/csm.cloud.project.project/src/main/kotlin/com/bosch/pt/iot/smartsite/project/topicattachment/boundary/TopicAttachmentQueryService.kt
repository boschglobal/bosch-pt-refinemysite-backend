/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.repository.RepositoryConstants
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TopicAttachmentQueryService(
    private val attachmentRepository: AttachmentRepository,
    private val topicAttachmentRepository: TopicAttachmentRepository
) {

  @Trace
  @PreAuthorize(
      "@topicAuthorizationComponent.hasViewPermissionOnTopicAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  fun findOneByIdentifier(attachmentIdentifier: UUID) =
      topicAttachmentRepository.findOneByIdentifier(attachmentIdentifier, AttachmentDto::class.java)
          ?: throw AggregateNotFoundException(
              ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#topicIdentifier)")
  @Transactional(readOnly = true)
  fun findAllByTopicIdentifier(topicIdentifier: TopicId) =
      topicAttachmentRepository.findAllByTopicIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
          topicIdentifier,
          RepositoryConstants.DEFAULT_ATTACHMENT_SORTING,
          AttachmentDto::class.java)

  @Trace
  @PreAuthorize("@topicAuthorizationComponent.hasViewPermissionOnTopic(#topicIdentifier)")
  @Transactional(readOnly = true)
  fun findAllByTopicIdentifierIncludingChildren(topicIdentifier: TopicId) =
      attachmentRepository.findAllByTopicIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
          topicIdentifier,
          RepositoryConstants.DEFAULT_ATTACHMENT_SORTING,
          AttachmentDto::class.java)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAllAndMappedByTopicIdentifierIn(topicIdentifiers: Collection<TopicId>) =
      topicAttachmentRepository
          .findAllByTopicIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
              topicIdentifiers,
              RepositoryConstants.DEFAULT_ATTACHMENT_SORTING,
              AttachmentDto::class.java)
          .groupBy { it.topicIdentifier!! }
}
