/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getFoundResponseEntity
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.AttachmentHelper.getRawData
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.DATA
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.ORIGINAL
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution.PREVIEW
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentService
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.TopicAttachmentListResource
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.TopicAttachmentResource
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory.TopicAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.resource.response.factory.TopicAttachmentResourceFactory
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.springframework.http.MediaType
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
class TopicAttachmentController(
    private val topicAttachmentResourceFactory: TopicAttachmentResourceFactory,
    private val topicAttachmentListResourceFactory: TopicAttachmentListResourceFactory,
    private val topicAttachmentQueryService: TopicAttachmentQueryService,
    private val topicAttachmentService: TopicAttachmentService,
    private val attachmentService: AttachmentService
) {

  @PostMapping(
      path = [ATTACHMENTS_BY_TOPIC_ID_ENDPOINT, ATTACHMENT_BY_TOPIC_ID_AND_ATTACHMENT_ID_ENDPOINT],
      consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  fun save(
      @RequestParam("file") file: MultipartFile,
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicIdentifier: TopicId,
      @PathVariable(value = PATH_VARIABLE_ATTACHMENT_ID, required = false)
      attachmentIdentifier: UUID?,
      @RequestParam(value = "zoneOffset") zoneOffset: ZoneOffset
  ): ResponseEntity<TopicAttachmentResource> {

    var fileName = file.originalFilename
    if (StringUtils.isBlank(fileName)) {
      fileName = DEFAULT_TOPIC_ATTACHMENT_NAME
    }
    val rawData = getRawData(file)
    val createdAttachmentIdentifier =
        topicAttachmentService.saveTopicAttachment(
            rawData,
            topicIdentifier,
            fileName!!,
            attachmentIdentifier,
            TimeZone.getTimeZone(zoneOffset))
    val attachment = topicAttachmentQueryService.findOneByIdentifier(createdAttachmentIdentifier)
    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(LinkUtils.getCurrentApiVersionPrefix())
            .path(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
            .buildAndExpand(createdAttachmentIdentifier)
            .toUri()
    return ResponseEntity.created(location).body(topicAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT)
  fun findOneByIdentifier(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ): ResponseEntity<TopicAttachmentResource> {
    val attachment = topicAttachmentQueryService.findOneByIdentifier(attachmentIdentifier)
    return ResponseEntity.ok(topicAttachmentResourceFactory.build(attachment))
  }

  @GetMapping(ATTACHMENTS_BY_TOPIC_ID_ENDPOINT)
  fun findAll(
      @PathVariable(PATH_VARIABLE_TOPIC_ID) topicIdentifier: TopicId,
      @RequestParam(value = "includeChildren", required = false, defaultValue = "false")
      includeChildren: Boolean
  ): ResponseEntity<TopicAttachmentListResource> {
    val attachments =
        if (includeChildren)
            topicAttachmentQueryService.findAllByTopicIdentifierIncludingChildren(topicIdentifier)
        else topicAttachmentQueryService.findAllByTopicIdentifier(topicIdentifier)
    return ResponseEntity.ok(topicAttachmentListResourceFactory.build(attachments))
  }

  @GetMapping(DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentPreview(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) = getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, PREVIEW))

  @GetMapping(DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentData(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) = getFoundResponseEntity(attachmentService.generateBlobAccessUrl(attachmentIdentifier, DATA))

  @GetMapping(DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT)
  fun downloadAttachmentOriginal(
      @PathVariable(PATH_VARIABLE_ATTACHMENT_ID) attachmentIdentifier: UUID
  ) =
      getFoundResponseEntity(
          attachmentService.generateBlobAccessUrl(attachmentIdentifier, ORIGINAL))

  companion object {
    const val DEFAULT_TOPIC_ATTACHMENT_NAME = "topic_attachment.jpg"
    const val ATTACHMENTS_BY_TOPIC_ID_ENDPOINT = "/projects/tasks/topics/{topicId}/attachments"
    const val ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/attachments/{attachmentId}"
    const val ATTACHMENT_BY_TOPIC_ID_AND_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/{topicId}/attachments/{attachmentId}"
    const val DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/attachments/{attachmentId}/data"
    const val DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/attachments/{attachmentId}/original"
    const val DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT =
        "/projects/tasks/topics/attachments/{attachmentId}/preview"
    const val PATH_VARIABLE_ATTACHMENT_ID = "attachmentId"
    const val PATH_VARIABLE_TOPIC_ID = "topicId"
  }
}
