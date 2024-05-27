/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.model

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RELATION
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecord

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = arrayOf(JoinColumn(nullable = false)),
        foreignKey = ForeignKey(name = "FK_Relation_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = arrayOf(JoinColumn(nullable = false)),
        foreignKey = ForeignKey(name = "FK_Relation_LastModifiedBy")))
@Table(
    indexes =
        [
            Index(name = "UK_Relation_Identifier", columnList = "identifier", unique = true),
            Index(
                name = "UK_Relation",
                columnList = "type,sourceIdentifier,sourceType,targetIdentifier,targetType",
                unique = true),
            Index(
                name = "UK_Relation_Search_All",
                columnList = "project_id,type,identifier",
                unique = true)])
class Relation(

    // type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    var type: RelationTypeEnum,

    // source
    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "identifier",
            column = Column(name = "sourceIdentifier", nullable = false, length = 36)),
        AttributeOverride(
            name = "type",
            column =
                Column(
                    name = "sourceType",
                    nullable = false,
                    length = 30,
                    columnDefinition = "varchar(30)")))
    var source: RelationElement,

    // target
    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "identifier",
            column = Column(name = "targetIdentifier", nullable = false, length = 36)),
        AttributeOverride(
            name = "type",
            column =
                Column(
                    name = "targetType",
                    nullable = false,
                    length = 30,
                    columnDefinition = "varchar(30)")))
    var target: RelationElement,

    // critical
    /** the criticality is always null for relation types other than FINISH_TO_START */
    @Column var critical: Boolean?,

    // project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_Relation_Project"))
    @ManyToOne(fetch = LAZY, optional = false)
    var project: Project
) : AbstractKafkaStreamable<Long, Relation, RelationEventEnumAvro>() {

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun getAggregateType() = RELATION.value

  override fun getDisplayName() = "$source ---($type)--> $target"

  override fun getChannel() = PROJECT_BINDING

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(eventType == DELETED).buildAggregateIdentifier(),
          project.identifier.toUuid())

  override fun toAvroMessage(): SpecificRecord {
    val relationAggregateAvro =
        RelationAggregateAvro.newBuilder()
            .setAggregateIdentifier(toAggregateIdentifier(eventType == DELETED))
            .setAuditingInformation(toAuditingInformationAvro(eventType == DELETED))
            .setType(RelationTypeEnumAvro.valueOf(type.name))
            .setSource(source.toAvro())
            .setTarget(target.toAvro())
            .setCritical(critical)
            .setProject(project.identifier.toAggregateReference())

    return RelationEventAvro.newBuilder()
        .setName(eventType)
        .setAggregateBuilder(relationAggregateAvro)
        .build()
  }

  private fun RelationElement.toAvro(): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setIdentifier(identifier.toString())
          .setType(type.name)
          .setVersion(0)
          .build()

  companion object {
    @JvmStatic
    fun newInstance() =
        Relation(
            FINISH_TO_START,
            RelationElement(randomUUID(), TASK),
            RelationElement(randomUUID(), TASK),
            null,
            Project())
  }
}
