/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.attachment.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext.PROJECT
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType.INTEGER
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType.SINGLE_TABLE
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType.TIMESTAMP
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.Date
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = INTEGER, length = 1)
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_TaskAttachment_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_TaskAttachment_LastModifiedBy")))
@Table(
    name = "task_attachment",
    indexes =
        [Index(name = "IX_TaskAttachment_Identifier", columnList = "identifier", unique = true)])
abstract class Attachment<T : Enum<*>, U : Attachment<T, U>> :
    AbstractKafkaStreamable<Long, U, T>, ImageBlobOwner {

  // Fields are marked as open to prevent the HibernateException with message:
  // "Getter methods of lazy classes cannot be final"
  // This seem to be a kotlin/java incompatibility which is not detected correctly
  // by the kotlin-spring/kotlin-jpa/kotlin-allopen plugin when the parent class
  // is written in kotlin and derived classes are written in java.
  // When the entire service is converted to kotlin, the open keyword can be removed.

  @field:NotNull
  @field:Size(min = 1, max = MAX_FILENAME_LENGTH)
  @Column(nullable = false, length = MAX_FILENAME_LENGTH)
  var fileName: String? = null

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskAttachment_Task"))
  @ManyToOne(fetch = LAZY, optional = false)
  var task: Task? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskAttachment_Topic"))
  @ManyToOne(fetch = LAZY)
  var topic: Topic? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskAttachment_Message"))
  @ManyToOne(fetch = LAZY)
  var message: Message? = null

  @Column(nullable = true) private var fullAvailable = false

  @Column(nullable = true) private var smallAvailable = false

  @Temporal(TIMESTAMP) var captureDate: Date? = null

  var fileSize: Long = 0

  var imageHeight: Long? = null

  var imageWidth: Long? = null

  constructor() : super()

  @JvmOverloads
  constructor(
      captureDate: Date?,
      fileName: String,
      fileSize: Long,
      imageHeight: Long,
      imageWidth: Long,
      task: Task,
      topic: Topic? = null,
      message: Message? = null
  ) {
    this.captureDate = captureDate
    this.fileName = fileName
    this.fileSize = fileSize
    this.imageHeight = imageHeight
    this.imageWidth = imageWidth
    this.task = task
    this.topic = topic
    this.message = message
  }

  override fun getDisplayName(): String? = fileName

  override fun isFullAvailable(): Boolean = fullAvailable

  override fun setFullAvailable(available: Boolean) {
    this.fullAvailable = available
  }

  override fun isSmallAvailable(): Boolean = smallAvailable

  override fun setSmallAvailable(available: Boolean) {
    this.smallAvailable = available
  }

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("captureDate", captureDate)
          .append("fullAvailable", fullAvailable)
          .append("fileName", fileName)
          .append("fileSize", fileSize)
          .append("imageHeight", imageHeight)
          .append("imageWidth", imageWidth)
          .append("smallAvailable", smallAvailable)
          .build()

  fun toAttachmentAvro(): AttachmentAvro =
      AttachmentAvro.newBuilder()
          .setCaptureDate(if (captureDate != null) captureDate!!.time else null)
          .setFullAvailable(isFullAvailable())
          .setFileName(fileName)
          .setFileSize(fileSize)
          .setHeight(imageHeight!!)
          .setSmallAvailable(isSmallAvailable())
          .setWidth(imageWidth!!)
          .build()

  override fun getBoundedContext(): BoundedContext = PROJECT

  override fun getIdentifierUuid() = requireNotNull(identifier)

  override fun getParentIdentifier(): UUID = task!!.identifier.toUuid()

  companion object {
    private const val MAX_FILENAME_LENGTH = 256
  }
}
