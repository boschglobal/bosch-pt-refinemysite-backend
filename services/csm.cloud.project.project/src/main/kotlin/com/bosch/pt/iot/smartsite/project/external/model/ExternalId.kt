/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.model

import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.EXTERNAL_ID
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdTypeEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.util.UUID
import org.apache.avro.specific.SpecificRecord
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ExternalId_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_ExternalId_LastModifiedBy")))
@Table(
    name = "EXTERNAl_ID",
    // We cannot add a unique constraint for the uniqueId and id, as a task, craft
    // and a working area (column) could have the same uniqueId.
    indexes =
        [Index(name = "IX_ExternalId_ProjType", columnList = "projectId, idType", unique = false)])
class ExternalId : AbstractKafkaStreamable<Long, ExternalId, ExternalIdEventEnumAvro> {

  @AttributeOverride(name = "identifier", column = Column(name = "projectId", nullable = false))
  lateinit var projectId: ProjectId

  @Column(nullable = false) lateinit var idType: ExternalIdType

  @Column(nullable = false) lateinit var objectIdentifier: UUID

  @Column(nullable = false, columnDefinition = "varchar(255)")
  @Enumerated(EnumType.STRING)
  lateinit var objectType: ObjectType

  // The guid is an externally managed UUID on elements in the (XML/proprietary) files
  var guid: UUID? = null

  // UniqueID (MS Project) = ObjectId (P6)
  var fileUniqueId: Int? = null

  // Id (MS Project) = Id (P6)
  var fileId: Int? = null

  // P6 activity id
  var activityId: String? = null

  // WBS
  var wbs: String? = null

  constructor() {
    // empty
  }

  // Constructor for MS Project / P6
  constructor(
      identifier: UUID,
      projectId: ProjectId,
      idType: ExternalIdType,
      // The identifier which is either the task, milestone or workArea identifier
      objectIdentifier: UUID,
      objectType: ObjectType,
      guid: UUID,
      uniqueId: Int,
      fileId: Int,
      activityId: String?,
      wbs: String?
  ) {
    this.identifier = identifier
    this.projectId = projectId
    this.idType = idType
    this.objectIdentifier = objectIdentifier
    this.objectType = objectType
    this.guid = guid
    this.fileUniqueId = uniqueId
    this.fileId = fileId
    this.activityId = activityId
    this.wbs = wbs
  }

  override fun getAggregateType(): String = EXTERNAL_ID.name

  override fun getDisplayName(): String = identifier.toString()

  override fun toString(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("identifier", identifier)
          .append("projectId", projectId)
          .append("idType", idType)
          .append("objectIdentifier", objectIdentifier)
          .append("objectType", objectType)
          .append("guid", guid)
          .append("uniqueId", fileUniqueId)
          .append("fileId", fileId)
          .append("activityId", activityId)
          .append("wbs", wbs)
          .toString()

  override fun toEvent(
      key: ByteArray,
      payload: ByteArray?,
      partition: Int,
      transactionId: UUID?
  ): AbstractKafkaEvent = ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toAvroMessage(): SpecificRecord =
      ExternalIdEventAvro(
          eventType,
          ExternalIdAggregateAvro(
              toAggregateIdentifier(DELETED == eventType),
              toAuditingInformationAvro(DELETED == eventType),
              projectId.toAggregateReference(),
              ExternalIdTypeEnumAvro.valueOf(idType.name),
              AggregateIdentifierAvro(objectIdentifier.toString(), 0L, objectType.type),
              guid.toString(),
              fileUniqueId,
              fileId,
              activityId,
              wbs))

  override fun toMessageKey(): EventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(DELETED == eventType).buildAggregateIdentifier(),
          projectId.toUuid())

  override fun getChannel(): String = PROJECT_BINDING

  companion object {
    private const val serialVersionUID: Long = 8059671650286701902
    const val MAX_WBS_LENGTH = 255
    const val MAX_ACTIVITY_ID_LENGTH = 255
  }
}
