/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.repository.RepositoryConstants.DEFAULT_ATTACHMENT_SORTING
import com.bosch.pt.iot.smartsite.common.repository.RepositoryConstants.DEFAULT_BULK_ATTACHMENT_SORTING
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class TaskAttachmentQueryService(
    private val attachmentRepository: AttachmentRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository
) {

  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasViewPermissionOnTaskAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  open fun findOneByIdentifier(attachmentIdentifier: UUID): AttachmentDto =
      taskAttachmentRepository.findOneByIdentifier(attachmentIdentifier, AttachmentDto::class.java)
          ?: throw AggregateNotFoundException(
              ATTACHMENT_VALIDATION_ERROR_NOT_FOUND, attachmentIdentifier.toString())

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllByTaskIdentifier(taskIdentifier: TaskId): List<AttachmentDto> =
      taskAttachmentRepository.findAllByTaskIdentifierAndTaskDeletedFalse(
          taskIdentifier, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findAllByTaskIdentifiers(taskIdentifiers: List<TaskId>): List<AttachmentDto> =
      taskAttachmentRepository.findAllByTaskIdentifierInAndTaskDeletedFalse(
          taskIdentifiers, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTask(#taskIdentifier)")
  @Transactional(readOnly = true)
  open fun findAllByTaskIdentifierIncludingChildren(taskIdentifier: TaskId): List<AttachmentDto> =
      attachmentRepository
          .findAllIdentifiersByTaskIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(taskIdentifier)
          .let {
            attachmentRepository.findAllByIdentifierIn(
                it, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)
          }

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasViewPermissionOnTasks(#taskIdentifiers)")
  @Transactional(readOnly = true)
  open fun findAllByTaskIdentifierInIncludingChildren(
      taskIdentifiers: Collection<TaskId>,
      pageable: Pageable
  ): Slice<AttachmentDto> {
    val sortedPageable: Pageable =
        PageRequest.of(pageable.pageNumber, pageable.pageSize, DEFAULT_BULK_ATTACHMENT_SORTING)

    val attachmentSlice =
        attachmentRepository
            .findAllIdentifiersByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
                taskIdentifiers, sortedPageable)
    val attachments =
        attachmentRepository.findAllByIdentifierIn(
            attachmentSlice.content, DEFAULT_BULK_ATTACHMENT_SORTING, AttachmentDto::class.java)

    return SliceImpl(attachments.toList(), attachmentSlice.pageable, attachmentSlice.hasNext())
  }

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  open fun findAllAndMappedByTaskIdentifierIn(
      taskIdentifiers: Collection<TaskId>
  ): Map<TaskId, List<AttachmentDto>> =
      taskAttachmentRepository
          .findAllByTaskIdentifierInAndTaskDeletedFalse(
              taskIdentifiers, DEFAULT_ATTACHMENT_SORTING, AttachmentDto::class.java)
          .groupBy { it.taskIdentifier }
}
