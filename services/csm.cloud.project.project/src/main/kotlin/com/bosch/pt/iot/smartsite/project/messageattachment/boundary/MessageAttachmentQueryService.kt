/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.repository.RepositoryConstants.DEFAULT_ATTACHMENT_SORTING
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MessageAttachmentQueryService(
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
  @Trace
  @PreAuthorize(
      "@messageAuthorizationComponent.hasViewPermissionOnMessageAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  fun findOneByIdentifier(attachmentIdentifier: UUID) =
      messageAttachmentRepository.findOneByIdentifier(
          attachmentIdentifier, AttachmentDto::class.java)
          ?: throw AggregateNotFoundException(
              ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())

  @Trace
  @PreAuthorize("@messageAuthorizationComponent.hasViewPermissionOnMessage(#messageIdentifier)")
  @Transactional(readOnly = true)
  fun findAllByMessageIdentifier(messageIdentifier: MessageId) =
      messageAttachmentRepository.findAllByMessageIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
          messageIdentifier, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAllAndMappedByMessageIdentifierIn(messageIdentifiers: Collection<MessageId>) =
      messageAttachmentRepository
          .findAllByMessageIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
              messageIdentifiers, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)
          .groupBy { it.messageIdentifier!! }
}
