/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.attachment.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.image.messages.ImageDeletedEventAvro
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.message.command.api.DeleteMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.handler.DeleteMessageCommandHandler
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.iot.smartsite.project.topic.command.api.DeleteTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.handler.DeleteTopicCommandHandler
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component

@Component
open class ImageDeletingProcessor(
    private val attachmentService: AttachmentService,
    private val deleteMessageCommandHandler: DeleteMessageCommandHandler,
    private val projectPictureService: ProjectPictureService,
    private val taskAttachmentService: TaskAttachmentService,
    private val deleteTopicCommandHandler: DeleteTopicCommandHandler
) : AbstractImageProcessor() {

  @Trace(operationName = "Process deleted image")
  open fun process(imageDeletedEvent: ImageDeletedEventAvro) =
      doProcess(
          imageDeletedEvent.filename.toUUID(),
          imageDeletedEvent.contentLength,
          imageDeletedEvent.path)

  protected fun doProcess(ownerIdentifier: UUID, originalFileSize: Long, path: String) {
    // Return if it's no file the project service is responsible for
    if (!PROJECT_CONTEXT_PICTURE_PATTERN.matcher(path).find()) {
      return
    }

    val projectPicturePath = PROJECT_PICTURE_PATTERN.matcher(path)
    if (projectPicturePath.find()) {

      val projectPicture = projectPictureService.findProjectPictureByIdentifier(ownerIdentifier)

      // Delete if all ids from image path match the identifiers from the database
      if (pathMatches(projectPicturePath, projectPicture?.project?.identifier.toString())) {
        projectPictureService.deleteProjectPictureWithoutAuthorization(ownerIdentifier)
      } else {
        logNotFound("Project picture", ownerIdentifier)
      }
      return
    }

    val attachment = attachmentService.findAttachment(ownerIdentifier)
    if (attachment == null) {
      logNotFound("Attachment", ownerIdentifier)
      return
    }

    val messageAttachmentPath = MESSAGE_ATTACHMENT_PATTERN.matcher(path)
    val topicAttachmentPath = TOPIC_ATTACHMENT_PATTERN.matcher(path)
    val taskAttachmentPath = TASK_ATTACHMENT_PATTERN.matcher(path)
    if (messageAttachmentPath.find()) {

      // Delete if all ids from image path match the identifiers from the database
      if (pathMatches(
          messageAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
          attachment.topic?.identifier.toString(),
          attachment.message?.identifier.toString(),
      )) {
        // Ideally we would introduce a message attachment deleted event.
        // As a simplification, we delete the entire message what deletes the attachment as well.
        deleteMessageCommandHandler.handleWithoutAuthorization(
            DeleteMessageCommand(checkNotNull(attachment.message?.identifier)))
      } else {
        logNotFound("Message attachment", ownerIdentifier)
      }
    } else if (topicAttachmentPath.find()) {

      // Delete if all ids from image path match the identifiers from the database
      if (pathMatches(
          topicAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
          attachment.topic?.identifier.toString(),
      )) {
        // Ideally we would introduce a topic attachment deleted event.
        // As a simplification, we delete the entire topic what deletes the attachment as well.
        deleteTopicCommandHandler.handle(
            DeleteTopicCommand(checkNotNull(attachment.topic?.identifier)))
      } else {
        logNotFound("Topic attachment", ownerIdentifier)
      }
    } else if (taskAttachmentPath.find()) {

      // Delete if all ids from image path match the identifiers from the database
      if (pathMatches(
          taskAttachmentPath,
          attachment.task?.project?.identifier.toString(),
          attachment.task?.identifier.toString(),
      )) {
        taskAttachmentService.deletePicture(ownerIdentifier)
      } else {
        logNotFound("Task attachment", ownerIdentifier)
      }
    }
  }
}
