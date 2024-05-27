/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.asRelationId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.Relation
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationReference
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.repository.RelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class RelationProjector(private val repository: RelationRepository) {

  fun onRelationEvent(aggregate: RelationAggregateAvro) {
    val existingRelation = repository.findOneByIdentifier(aggregate.getIdentifier().asRelationId())

    if (existingRelation == null || aggregate.getVersion() > existingRelation.version) {
      (existingRelation?.updateFromRelationAggregate(aggregate) ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onRelationDeletedEvent(aggregate: RelationAggregateAvro) {
    val relation = repository.findOneByIdentifier(aggregate.getIdentifier().asRelationId())
    if (relation != null && !relation.deleted) {
      val newVersion =
          relation.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          RelationMapper.INSTANCE.fromRelationVersion(
              newVersion,
              relation.identifier,
              relation.project,
              relation.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun RelationAggregateAvro.toNewProjection(): Relation {
    val relationVersion = this.newRelationVersion()

    return RelationMapper.INSTANCE.fromRelationVersion(
        relationVersion = relationVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asRelationId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(relationVersion))
  }

  private fun Relation.updateFromRelationAggregate(aggregate: RelationAggregateAvro): Relation {
    val relationVersion = aggregate.newRelationVersion()

    return RelationMapper.INSTANCE.fromRelationVersion(
        relationVersion = relationVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(relationVersion) })
  }

  private fun RelationAggregateAvro.newRelationVersion(): RelationVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return RelationVersion(
        version = this.aggregateIdentifier.version,
        critical = this.critical ?: false,
        type = RelationTypeEnum.valueOf(this.type.name),
        source = RelationReference(this.source.identifier.toUUID(), this.source.type),
        target = RelationReference(this.target.identifier.toUUID(), this.target.type),
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
