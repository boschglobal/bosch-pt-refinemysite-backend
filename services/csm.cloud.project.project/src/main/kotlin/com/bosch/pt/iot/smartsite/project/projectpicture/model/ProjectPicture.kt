/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectpicture.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType.PROJECT_PICTURE
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext.PROJECT
import com.bosch.pt.csm.cloud.common.blob.model.ImageBlobOwner
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTPICTURE
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ProjectPicture_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ProjectPicture_LastModifiedBy")))
@Table(
    indexes =
        [Index(name = "UK_ProjectPicture_ProjectId", columnList = "project_id", unique = true)])
class ProjectPicture :
    AbstractKafkaStreamable<Long, ProjectPicture, ProjectPictureEventEnumAvro>, ImageBlobOwner {

  @field:NotNull
  @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjectPicture_Project"))
  @OneToOne(fetch = LAZY, optional = false)
  var project: Project? = null

  @Column private var smallAvailable = false

  @Column private var fullAvailable = false

  @field:NotNull @Column(nullable = false) var width: Long? = null

  @field:NotNull @Column(nullable = false) var height: Long? = null

  @field:NotNull @Column(nullable = false) var fileSize: Long? = null

  constructor()

  constructor(project: Project, fileSize: Long) {
    this.project = project
    this.fileSize = fileSize
    this.width = 0
    this.height = 0
  }

  override fun getDisplayName(): String = "Profile Picture"

  override fun isSmallAvailable(): Boolean = smallAvailable

  override fun setSmallAvailable(available: Boolean) {
    this.smallAvailable = available
  }

  override fun isFullAvailable(): Boolean = fullAvailable

  override fun setFullAvailable(available: Boolean) {
    this.fullAvailable = available
  }

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toAvroMessage(): SpecificRecord =
      ProjectPictureEventAvro.newBuilder()
          .setName(eventType)
          .setAggregateBuilder(
              ProjectPictureAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifier(DELETED == eventType))
                  .setAuditingInformation(toAuditingInformationAvro(DELETED == eventType))
                  .setProject(project!!.identifier.toAggregateReference())
                  .setSmallAvailable(smallAvailable)
                  .setFullAvailable(fullAvailable)
                  .setFileSize(fileSize!!)
                  .setHeight(height!!)
                  .setWidth(width!!))
          .build()

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(DELETED == eventType).buildAggregateIdentifier(),
          project!!.identifier.toUuid())

  override fun getChannel(): String = PROJECT_BINDING

  override fun getBoundedContext(): BoundedContext = PROJECT

  override fun getParentIdentifier(): UUID = project!!.identifier.toUuid()

  override fun getIdentifierUuid(): UUID = identifier!!

  override fun getOwnerType(): BlobMetadata.OwnerType = PROJECT_PICTURE

  override fun getAggregateType(): String = PROJECTPICTURE.value

  companion object {
    private const val serialVersionUID: Long = 4986427058415300546
  }
}
