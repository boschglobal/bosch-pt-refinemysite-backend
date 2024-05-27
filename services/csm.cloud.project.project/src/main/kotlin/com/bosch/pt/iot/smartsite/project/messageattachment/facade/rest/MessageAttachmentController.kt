/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getFoundResponseEntity
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getRawData
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.DATA
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.ORIGINAL
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.PREVIEW
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentService
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentListResource
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.MessageAttachmentResource
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory.MessageAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.resource.response.factory.MessageAttachmentResourceFactory
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@RestController
class MessageAttachmentController(
    private val attachmentService: AttachmentService,
    private val messageAttachmentService: MessageAttachmentService,
    private val messageAttachmentQueryService: MessageAttachmentQueryService,
    private val messageAttachmentResourceFactory: MessageAttachmentResourceFactory,
    private val messageAttachmentListResourceFactory: MessageAttachmentListResourceFactory
) {

  @PostMapping(
      path =
          [ATTACHMENT_BY_MESSAGE_ID_AND_ATTACHMENT_ID_ENDPOINT, ATTACHMENTS_BY_MESSAGE_ID_ENDPOINT],
      consumes = [MULTIPART_FORM_DATA_VALUE])
  fun save(
      @RequestParam("file") file: MultipartFile,
      @PathVariable(PATH_VARIABLE_MESSAGE_ID) messageIdentifier: MessageId,
      @PathVariable(value = PATH_VARIABLE_ATTACHMENT_ID, required = false)
      attachmentIdentifier: UUID?,
      @RequestParam(value = "zoneOffset") zoneOffset: ZoneOffset
  ): ResponseEntity<MessageAttachmentResource> {

    val fileName =
        if (StringUtils.isBlank(file.originalFilename)) DEFAULT_MESSAGE_ATTACHMENT_NAME
        else file.originalFilename

    val rawData = getRawData(file)
    val createdAttachmentIdentifier =
        messageAttachmentService.saveMessageAttachment(
            rawData,
            messageIdentifier,
            fileName,
            attachmentIdentifier,
            TimeZone.getTimeZone(zoneOffset))
    val attachment = messageAttachmentQueryService.findOneByIdentifier(createdAttachmentIdentifier)
    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix())
            .path(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .buildAndExpand(createdAttachmentIdentifier)
            .toUri()
    return ResponseEntity.created(location).body(messageAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  fun findOneByIdentifier(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<MessageAttachmentResource> {
    val attachment = messageAttachmentQueryService.findOneByIdentifier(attachmentIdentifier)
    return ResponseEntity.ok(messageAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENTS_BY_MESSAGE_ID_ENDPOINT)
  fun findAll(
      @PathVariable(PATH_VARIABLE_MESSAGE_ID) messageIdentifier: MessageId
  ): ResponseEntity<MessageAttachmentListResource> {
    val attachments = messageAttachmentQueryService.findAllByMessageIdentifier(messageIdentifier)
    return ResponseEntity.ok(messageAttachmentListResourceFactory.build(attachments))
  }

  @GetMapping(PREVIEW_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentPreview(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) = getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, PREVIEW))

  @GetMapping(FULL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentData(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) = getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, DATA))

  @GetMapping(ORIGINAL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentOriginal(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) =
      getFoundResponseEntity(
          attachmentService.generateBlobAccessUrl(attachmentIdentifier, ORIGINAL))

  companion object {
    const val DEFAULT_MESSAGE_ATTACHMENT_NAME = "msg_attachment.jpg"
    const val ATTACHMENTS_BY_MESSAGE_ID_ENDPOINT =
        "/projects/tasks/topics/messages/{messageId}/attachments"
    const val ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/messages/attachments/{attachmentId}"
    const val ATTACHMENT_BY_MESSAGE_ID_AND_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/messages/{messageId}/attachments/{attachmentId}"
    const val FULL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/messages/attachments/{attachmentId}/data"
    const val PREVIEW_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/messages/attachments/{attachmentId}/preview"
    const val ORIGINAL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/messages/attachments/{attachmentId}/original"
    const val PATH_VARIABLE_ATTACHMENT_ID = "attachmentId"
    const val PATH_VARIABLE_MESSAGE_ID = "messageId"
  }
}
