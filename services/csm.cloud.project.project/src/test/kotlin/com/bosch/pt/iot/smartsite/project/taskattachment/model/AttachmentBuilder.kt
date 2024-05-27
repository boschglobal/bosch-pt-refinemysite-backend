/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.model

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.model.MessageBuilder.Companion.message
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicBuilder.Companion.topic
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.util.Date
import java.util.Random
import java.util.UUID

class AttachmentBuilder private constructor() {
  private var captureDate: Date? = null
  private var createdBy: User? = null
  private var createdDate: LocalDateTime? = null
  private var fileName: String? = null
  private var fileSize = 0
  private var identifier: UUID? = null
  private var imageHeight: Long? = null
  private var imageWidth: Long? = null
  private var topic: Topic? = null
  private var message: Message? = null
  private var lastModifiedBy: User? = null
  private var lastModifiedDate: LocalDateTime? = null
  private var resolutionsAvailable = emptySet<AttachmentImageResolution>()
  private var task: Task? = null

  fun withCaptureDate(captureDate: Date?): AttachmentBuilder = apply {
    this.captureDate = captureDate
  }

  fun withCreatedBy(createdBy: User?): AttachmentBuilder = apply { this.createdBy = createdBy }

  fun withCreatedDate(createdDate: LocalDateTime?): AttachmentBuilder = apply {
    this.createdDate = createdDate
  }

  fun withFileName(fileName: String?): AttachmentBuilder = apply { this.fileName = fileName }

  fun withFileSize(fileSize: Int): AttachmentBuilder = apply { this.fileSize = fileSize }

  fun withIdentifier(identifier: UUID?): AttachmentBuilder = apply { this.identifier = identifier }

  fun withImageHeight(imageHeight: Long?): AttachmentBuilder = apply {
    this.imageHeight = imageHeight
  }

  fun withImageWidth(imageWidth: Long?): AttachmentBuilder = apply { this.imageWidth = imageWidth }

  fun withTopic(topic: Topic?): AttachmentBuilder = apply { this.topic = topic }

  fun withMessage(message: Message?): AttachmentBuilder = apply { this.message = message }

  fun withLastModifiedBy(lastModifiedBy: User?): AttachmentBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): AttachmentBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun withResolutionsAvailable(vararg resolutions: AttachmentImageResolution): AttachmentBuilder =
      apply {
        resolutionsAvailable = HashSet(listOf(*resolutions))
      }

  fun withTask(task: Task?): AttachmentBuilder = apply { this.task = task }

  fun asTaskAttachment(): AttachmentBuilder = apply {
    this.topic = null
    this.message = null
  }

  fun asTopicAttachment(): AttachmentBuilder = apply { this.message = null }

  fun asMessageAttachment(): AttachmentBuilder = this

  fun build(): Attachment<*, *> {
    val attachment: Attachment<*, *> =
        if (message != null) {
          MessageAttachment(
              captureDate,
              fileName!!,
              fileSize.toLong(),
              imageHeight!!,
              imageWidth!!,
              task!!,
              topic!!,
              message!!)
        } else if (topic != null) {
          TopicAttachment(
              captureDate,
              fileName!!,
              fileSize.toLong(),
              imageHeight!!,
              imageWidth!!,
              task!!,
              topic)
        } else {
          TaskAttachment(
              captureDate, fileName!!, fileSize.toLong(), imageHeight!!, imageWidth!!, task!!)
        }
    resolutionsAvailable.stream().map(AttachmentImageResolution::imageResolution).forEach {
        imageResolution: ImageResolution? ->
      attachment.setResolutionAvailable(imageResolution!!)
    }
    if (createdBy != null) {
      attachment.setCreatedBy(createdBy)
    }
    if (createdDate != null) {
      attachment.setCreatedDate(createdDate!!)
    }
    if (lastModifiedDate != null) {
      attachment.setLastModifiedDate(lastModifiedDate!!)
    }
    if (identifier != null) {
      attachment.identifier = identifier
    }
    if (lastModifiedBy != null) {
      attachment.setLastModifiedBy(lastModifiedBy)
    }
    return attachment
  }

  fun buildTaskAttachment(): TaskAttachment {
    val attachment =
        TaskAttachment(
            captureDate, fileName!!, fileSize.toLong(), imageHeight!!, imageWidth!!, task!!)
    resolutionsAvailable.stream().map(AttachmentImageResolution::imageResolution).forEach {
        imageResolution: ImageResolution? ->
      attachment.setResolutionAvailable(imageResolution!!)
    }
    if (createdBy != null) {
      attachment.setCreatedBy(createdBy)
    }
    if (createdDate != null) {
      attachment.setCreatedDate(createdDate!!)
    }
    if (identifier != null) {
      attachment.identifier = identifier
    }
    if (lastModifiedBy != null) {
      attachment.setLastModifiedBy(lastModifiedBy)
    }
    if (lastModifiedDate != null) {
      attachment.setLastModifiedDate(lastModifiedDate!!)
    }
    return attachment
  }

  fun buildTopicAttachment(): TopicAttachment {
    val attachment =
        TopicAttachment(
            captureDate, fileName!!, fileSize.toLong(), imageHeight!!, imageWidth!!, task!!, topic)
    resolutionsAvailable.stream().map(AttachmentImageResolution::imageResolution).forEach {
        imageResolution: ImageResolution? ->
      attachment.setResolutionAvailable(imageResolution!!)
    }
    if (createdBy != null) {
      attachment.setCreatedBy(createdBy)
    }
    if (createdDate != null) {
      attachment.setCreatedDate(createdDate!!)
    }
    if (identifier != null) {
      attachment.identifier = identifier
    }
    if (lastModifiedBy != null) {
      attachment.setLastModifiedBy(lastModifiedBy)
    }
    if (lastModifiedDate != null) {
      attachment.setLastModifiedDate(lastModifiedDate!!)
    }
    return attachment
  }

  fun buildMessageAttachment(): MessageAttachment {
    val attachment =
        MessageAttachment(
            captureDate,
            fileName!!,
            fileSize.toLong(),
            imageHeight!!,
            imageWidth!!,
            task!!,
            topic!!,
            message!!)
    resolutionsAvailable.stream().map(AttachmentImageResolution::imageResolution).forEach {
        imageResolution: ImageResolution? ->
      attachment.setResolutionAvailable(imageResolution!!)
    }
    if (createdBy != null) {
      attachment.setCreatedBy(createdBy)
    }
    if (createdDate != null) {
      attachment.setCreatedDate(createdDate!!)
    }
    if (identifier != null) {
      attachment.identifier = identifier
    }
    if (lastModifiedBy != null) {
      attachment.setLastModifiedBy(lastModifiedBy)
    }
    if (lastModifiedDate != null) {
      attachment.setLastModifiedDate(lastModifiedDate!!)
    }
    return attachment
  }

  companion object {
    fun attachment(): AttachmentBuilder = AttachmentBuilder()
  }

  /** Private constructor to initialize builder. */
  init {
    val task = task().withIdentifier(TaskId()).build()
    val topic = topic().withTask(task).withIdentifier(UUID.randomUUID()).build()
    val message = message().withTopic(topic).withIdentifier(MessageId()).build()
    val now = LocalDateTime.now()
    withCaptureDate(Date())
        .withCreatedDate(now)
        .withLastModifiedDate(now)
        .withFileName(UUID.randomUUID().toString())
        .withFileSize(Random(1000000).nextInt())
        .withIdentifier(UUID.randomUUID())
        .withImageHeight(800L)
        .withImageWidth(800L)
        .withTopic(topic)
        .withMessage(message)
        .withTask(task)
  }
}
