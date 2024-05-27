/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

@file:Suppress("SwallowedException")

package com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService
import com.bosch.pt.csm.cloud.projectmanagement.attachment.service.AttachmentService
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.DeletedAttachmentUriBuilder.buildDeletedAttachmentUri
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus.FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
class AttachmentController(
    protected var attachmentService: AttachmentService,
    private val activityService: ActivityService
) {

  @GetMapping(ATTACHMENT_BY_ID_ENDPOINT)
  fun downloadAttachment(
      @PathVariable(PARAM_ATTACHMENT_ID) attachmentId: UUID,
      @PathVariable(PARAM_SIZE) resolution: ImageResolution
  ): ResponseEntity<*> {
    val activity: Activity =
        activityService.findActivityByAttachmentIdentifier(attachmentId)
            ?: throw ResourceNotFoundException(Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)

    val attachment = activity.attachment
    return try {
      val blobUrl =
          attachmentService.generateBlobAccessUrl(
              BoundedContext.PROJECT, activity.context.task, attachment!!.identifier, resolution)
      ResponseEntity.status(FOUND).header(LOCATION, blobUrl.toString()).build<Any>()
    } catch (e: ResourceNotFoundException) {
      LOGGER.debug("Attachment of activity not found, use default picture instead", e)

      buildDeletedAttachmentUri(resolution).let {
        ResponseEntity.status(FOUND).header(LOCATION, it.toString()).build<Any>()
      }
    }
  }
  companion object {
    private val LOGGER = LoggerFactory.getLogger(AttachmentController::class.java)
    const val PARAM_ATTACHMENT_ID = "attachmentId"
    const val PARAM_SIZE = "size"
    const val ATTACHMENT_BY_ID_ENDPOINT =
        "/projects/tasks/activities/attachments/{attachmentId}/{size}"
  }
}
