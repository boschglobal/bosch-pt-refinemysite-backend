/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getFoundResponseEntity
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getRawData
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.DATA
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.ORIGINAL
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.PREVIEW
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentBatchResource
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentListResource
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentResourceFactory
import jakarta.validation.Valid
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
open class TaskAttachmentController(
    private val attachmentService: AttachmentService,
    private val taskAttachmentService: TaskAttachmentService,
    private val taskAttachmentQueryService: TaskAttachmentQueryService,
    private val taskAttachmentResourceFactory: TaskAttachmentResourceFactory,
    private val taskAttachmentListResourceFactory: TaskAttachmentListResourceFactory,
    private val taskAttachmentBatchResourceFactory: TaskAttachmentBatchResourceFactory
) {

  @PostMapping(
      path = [ATTACHMENTS_BY_TASK_ID_ENDPOINT, ATTACHMENT_BY_TASK_ID_AND_ATTACHMENT_ID_ENDPOINT],
      consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  open fun save(
      @RequestParam("file") file: MultipartFile,
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @PathVariable(name = PATH_VARIABLE_ATTACHMENT_ID, required = false)
      attachmentIdentifier: UUID?,
      @RequestParam(value = "zoneOffset") zoneOffset: ZoneOffset
  ): ResponseEntity<TaskAttachmentResource> {

    var fileName = file.originalFilename
    if (StringUtils.isBlank(fileName)) {
      fileName = DEFAULT_TASK_ATTACHMENT_NAME
    }
    val rawData = getRawData(file)
    val createdAttachmentIdentifier =
        taskAttachmentService.saveTaskAttachment(
            rawData,
            taskIdentifier,
            fileName!!,
            attachmentIdentifier,
            TimeZone.getTimeZone(zoneOffset))
    val attachment = taskAttachmentQueryService.findOneByIdentifier(createdAttachmentIdentifier)
    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .buildAndExpand(createdAttachmentIdentifier)
            .toUri()
    return ResponseEntity.created(location).body(taskAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  open fun findOneByIdentifier(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<TaskAttachmentResource> {
    val attachment = taskAttachmentQueryService.findOneByIdentifier(attachmentIdentifier)
    return ResponseEntity.ok(taskAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENTS_BY_TASK_ID_ENDPOINT)
  open fun findAll(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @RequestParam(value = "includeChildren", required = false, defaultValue = "false")
      includeChildren: Boolean
  ): ResponseEntity<TaskAttachmentListResource> {
    val attachments =
        if (includeChildren)
            taskAttachmentQueryService.findAllByTaskIdentifierIncludingChildren(taskIdentifier)
        else taskAttachmentQueryService.findAllByTaskIdentifier(taskIdentifier)
    return ResponseEntity.ok(taskAttachmentListResourceFactory.build(attachments, taskIdentifier))
  }

  @PostMapping(ATTACHMENTS_ENDPOINT)
  open fun findAllByTaskIdentifiers(
      @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @PageableDefault(size = 100) pageable: Pageable,
      @RequestParam(name = "identifierType", defaultValue = TASK) identifierType: String
  ): ResponseEntity<TaskAttachmentBatchResource> {
    return if (identifierType == TASK) {
      val taskIdentifiers = batchRequestResource.ids.asTaskIds()
      val attachments =
          taskAttachmentQueryService.findAllByTaskIdentifierInIncludingChildren(
              taskIdentifiers, pageable)
      ResponseEntity.ok(taskAttachmentBatchResourceFactory.build(attachments))
    } else {
      throw BatchIdentifierTypeNotSupportedException(
          COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
    }
  }

  @DeleteMapping(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  open fun delete(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<Void> {
    taskAttachmentService.deleteTaskAttachmentByIdentifier(attachmentIdentifier)
    return ResponseEntity.noContent().build()
  }

  @GetMapping(DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT)
  open fun downloadAttachmentPreview(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<Void> =
      getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, PREVIEW))

  @GetMapping(DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT)
  open fun downloadAttachmentData(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<Void> =
      getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, DATA))

  @GetMapping(DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT)
  open fun downloadAttachmentOriginal(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<Void> =
      getFoundResponseEntity(
          attachmentService.generateBlobAccessUrl(attachmentIdentifier, ORIGINAL))

  companion object {
    const val DEFAULT_TASK_ATTACHMENT_NAME = "task_attachment.jpg"
    const val ATTACHMENTS_ENDPOINT = "/projects/tasks/attachments"
    const val ATTACHMENTS_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/attachments"
    const val ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT = "/projects/tasks/attachments/{attachmentId}"
    const val ATTACHMENT_BY_TASK_ID_AND_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/{taskId}/attachments/{attachmentId}"
    const val DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/attachments/{attachmentId}/data"
    const val DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/attachments/{attachmentId}/original"
    const val DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/attachments/{attachmentId}/preview"
    const val PATH_VARIABLE_ATTACHMENT_ID = "attachmentId"
    const val PATH_VARIABLE_TASK_ID = "taskId"
  }
}
