/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import datadog.trace.api.Trace
import java.net.URL
import java.util.TimeZone
import java.util.UUID
import java.util.function.Consumer
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

@Service
open class TaskAttachmentService(
    private val idGenerator: IdGenerator,
    private val taskRepository: TaskRepository,
    private val attachmentRepository: AttachmentRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository,
    private val attachmentService: AttachmentService
) {

  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasViewPermissionOnTaskAttachment(#attachmentIdentifier)")
  @Transactional(readOnly = true)
  open fun generateBlobAccessUrl(
      attachmentIdentifier: UUID,
      attachmentImageResolution: AttachmentImageResolution
  ): URL = attachmentService.generateBlobAccessUrl(attachmentIdentifier, attachmentImageResolution)

  @Trace
  @PreAuthorize("@taskAuthorizationComponent.hasEditPermissionOnTask(#taskIdentifier)")
  @Transactional
  open fun saveTaskAttachment(
      binaryData: ByteArray,
      taskIdentifier: TaskId,
      fileName: String,
      attachmentIdentifier: UUID?,
      timeZone: TimeZone
  ): UUID {

    val task =
        taskRepository.findOneByIdentifier(taskIdentifier)
            ?: throw AggregateNotFoundException(
                TASK_VALIDATION_ERROR_NOT_FOUND, taskIdentifier.toString())

    var taskAttachment = createTaskAttachment(fileName, binaryData.size.toLong(), task)
    taskAttachment.identifier = attachmentIdentifier ?: idGenerator.generateId()
    taskAttachment = taskAttachmentRepository.save(taskAttachment, CREATED)
    attachmentService.storeBlob(
        binaryData, taskAttachment, BlobMetadata.from(fileName, timeZone, taskAttachment))
    return taskAttachment.identifier!!
  }

  @NoPreAuthorize
  @Transactional
  open fun updateImageMetadata(
      attachmentIdentifier: UUID,
      originalFileSize: Long,
      imageMetadata: ImageMetadataDto?
  ) =
      attachmentService.updateImageMetadata(
          attachmentIdentifier, originalFileSize, imageMetadata, UPDATED)

  @Trace
  @Transactional
  @NoPreAuthorize
  open fun deletePicture(attachmentIdentifier: UUID) =
      attachmentService.deleteAttachmentIfExists(attachmentIdentifier, DELETED)

  @Trace
  @PreAuthorize(
      "@taskAuthorizationComponent.hasDeletePermissionOnTaskAttachment(#attachmentIdentifier)")
  @Transactional
  open fun deleteTaskAttachmentByIdentifier(attachmentIdentifier: UUID) {
    val attachment =
        attachmentService.findAttachmentOrThrowException(attachmentIdentifier) as TaskAttachment
    attachmentService.deleteAllResolutionsOfImageIfExists(attachment)
    attachmentRepository.delete(attachment, DELETED)
  }

  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deletePartitioned(taskIds: List<Long>) {
    val attachments = taskAttachmentRepository.getByTaskIdsPartitioned(taskIds)
    attachments.forEach(
        Consumer { blobOwner: TaskAttachment ->
          attachmentService.deleteAllResolutionsOfImageIfExists(blobOwner)
        })
    taskAttachmentRepository.deletePartitioned(attachments.map { obj: TaskAttachment -> obj.id!! })
  }

  private fun createTaskAttachment(fileName: String, fileSize: Long, task: Task): TaskAttachment =
      TaskAttachment(null, fileName, fileSize, 0, 0, task)
}
