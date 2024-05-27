/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.Companion.KEY_TIMEZONE
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution.ORIGINAL
import com.bosch.pt.csm.cloud.common.blob.repository.AzureBlobStorageRepository
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.image.messages.ImageScaledEventAvro
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.attachment.dto.ImageMetadataDto
import com.bosch.pt.iot.smartsite.attachment.util.ImageMetadataExtractor
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentService
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentService
import datadog.trace.api.Trace
import java.util.TimeZone
import java.util.UUID
import org.springframework.stereotype.Component

@Component
open class ImageScalingProcessor(
    private val attachmentService: AttachmentService,
    private val azureBlobStorageRepository: AzureBlobStorageRepository,
    private val blobStorageService: BlobStoreService,
    private val imageMetadataExtractor: ImageMetadataExtractor,
    private val messageAttachmentService: MessageAttachmentService,
    private val projectPictureService: ProjectPictureService,
    private val taskAttachmentService: TaskAttachmentService,
    private val topicAttachmentService: TopicAttachmentService,
) : AbstractImageProcessor() {

  @Trace(operationName = "Process scaled image")
  open fun process(imageScaledEvent: ImageScaledEventAvro) =
      doProcess(
          imageScaledEvent.filename.toUUID(), imageScaledEvent.contentLength, imageScaledEvent.path)

  private fun doProcess(ownerIdentifier: UUID, originalFileSize: Long, path: String) {
    if (!PROJECT_CONTEXT_PICTURE_PATTERN.matcher(path).find()) {
      // It's no file the project service is responsible for
      return
    }

    val projectPicturePath = PROJECT_PICTURE_PATTERN.matcher(path)
    val messageAttachmentPath = MESSAGE_ATTACHMENT_PATTERN.matcher(path)
    val topicAttachmentPath = TOPIC_ATTACHMENT_PATTERN.matcher(path)
    val taskAttachmentPath = TASK_ATTACHMENT_PATTERN.matcher(path)

    if (projectPicturePath.find()) {

      val projectPicture = projectPictureService.findProjectPictureByIdentifier(ownerIdentifier)

      if (pathMatches(projectPicturePath, projectPicture?.project?.identifier.toString())) {
        val blobName = blobStorageService.buildBlobName(projectPicture!!, ORIGINAL)
        val metadata = readImageMetadata(blobName)
        projectPictureService.updateImageMetadata(ownerIdentifier, originalFileSize, metadata)
      } else {
        logNotFound("Profile picture", ownerIdentifier)
      }
      return
    }

    val attachment = attachmentService.findAttachment(ownerIdentifier)
    if (attachment == null) {
      logNotFound("Attachment", ownerIdentifier)
      return
    }
    val blobName = blobStorageService.buildBlobName(attachment, ORIGINAL)

    if (messageAttachmentPath.find()) {
      if (pathMatches(
          messageAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
          attachment.topic?.identifier.toString(),
          attachment.message?.identifier.toString(),
      )) {
        val metadata = readImageMetadata(blobName)
        messageAttachmentService.updateImageMetadata(ownerIdentifier, originalFileSize, metadata)
      } else {
        logNotFound("Message attachment", ownerIdentifier)
      }
    } else if (topicAttachmentPath.find()) {
      if (pathMatches(
          topicAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
          attachment.topic?.identifier.toString(),
      )) {
        val metadata = readImageMetadata(blobName)
        topicAttachmentService.updateImageMetadata(ownerIdentifier, originalFileSize, metadata)
      } else {
        logNotFound("Topic attachment", ownerIdentifier)
      }
    } else if (taskAttachmentPath.find()) {
      if (pathMatches(
          taskAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
      )) {
        val metadata = readImageMetadata(blobName)
        taskAttachmentService.updateImageMetadata(ownerIdentifier, originalFileSize, metadata)
      } else {
        logNotFound("Task attachment", ownerIdentifier)
      }
    }
  }

  private fun readImageMetadata(blobName: String): ImageMetadataDto? =
      azureBlobStorageRepository.getBlockBlobClient(blobName).let {
        if (!it.exists()) {
          return@let null
        }

        val timezone =
            it.tags[KEY_TIMEZONE].let { timezoneString ->
              TimeZone.getTimeZone(timezoneString ?: "")
            }

        val blob = it.openInputStream().use { it.readAllBytes() }
        imageMetadataExtractor.readMetadata(blob, timezone)
      }
}
