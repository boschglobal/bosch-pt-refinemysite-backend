/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.relation.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RELATION
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.UNCRITICAL
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RelationRestoreStrategy(
    private val relationRepository: RelationRepository,
    private val projectRepository: ProjectRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, relationRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      RELATION.value == record.key().aggregateIdentifier.type &&
          record.value() is RelationEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val key = record.key()
    val event = record.value() as RelationEventAvro?
    assertEventNotNull(event, key)

    when (event!!.name) {
      CREATED -> createRelation(event.aggregate)
      DELETED ->
          deleteRelation(
              key.aggregateIdentifier.identifier, key.rootContextIdentifier.asProjectId())
      CRITICAL -> updateRelation(event.aggregate, key.rootContextIdentifier.asProjectId())
      UNCRITICAL -> updateRelation(event.aggregate, key.rootContextIdentifier.asProjectId())
      else -> handleInvalidEventType(event.name.name)
    }
  }

  private fun createRelation(aggregate: RelationAggregateAvro) {
    val relation = Relation.newInstance()

    setAttributes(relation, aggregate)
    setAuditAttributes(relation, aggregate.auditingInformation)

    entityManager.persist(relation)
  }

  private fun updateRelation(
      relationAggregate: RelationAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      findRelation(relationAggregate.getIdentifier(), projectIdentifier).also {
        update(
            it,
            object : DetachedEntityUpdateCallback<Relation> {
              override fun update(entity: Relation) {
                setAttributes(entity, relationAggregate)
                setAuditAttributes(entity, relationAggregate.auditingInformation)
              }
            })
      }

  private fun deleteRelation(identifier: UUID, projectIdentifier: ProjectId) =
      delete(
          relationRepository.findOneByIdentifierAndProjectIdentifier(identifier, projectIdentifier))

  private fun setAttributes(relation: Relation, aggregate: RelationAggregateAvro) {
    with(aggregate) {
      relation.identifier = getIdentifier()
      relation.version = getVersion()
      relation.type = RelationTypeEnum.valueOf(type.name)
      relation.source = source.toRelationElement()
      relation.target = target.toRelationElement()
      relation.critical = critical
      relation.project = findProject(project)
    }
  }

  private fun findProject(aggregateIdentifier: AggregateIdentifierAvro): Project =
      requireNotNull(
          projectRepository.findOneByIdentifier(aggregateIdentifier.identifier.asProjectId())) {
            "Project missing: ${aggregateIdentifier.identifier}"
          }

  private fun findRelation(relationIdentifier: UUID, projectIdentifier: ProjectId) =
      requireNotNull(
          relationRepository.findOneByIdentifierAndProjectIdentifier(
              relationIdentifier, projectIdentifier))

  private fun AggregateIdentifierAvro.toRelationElement() =
      RelationElement(identifier.toUUID(), RelationElementTypeEnum.valueOf(type))
}
