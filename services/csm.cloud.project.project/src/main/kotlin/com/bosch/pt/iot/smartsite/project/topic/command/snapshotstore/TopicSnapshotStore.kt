/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.iot.smartsite.common.i18n.Key.TOPIC_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.message.boundary.MessageDeleteService
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.boundary.TopicAttachmentService
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class TopicSnapshotStore(
    private val snapshotCache: TopicSnapshotEntityCache,
    private val repository: TopicRepository,
    private val taskRepository: TaskRepository,
    private val messageDeleteService: MessageDeleteService,
    private val topicAttachmentService: TopicAttachmentService,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<TopicEventG2Avro, TopicSnapshot, Topic, TopicId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: TopicId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              TOPIC_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  fun findAllOrIgnore(identifiers: List<TopicId>) =
      snapshotCache.populateFromCall { snapshotCache.loadAllFromDatabase(identifiers) }

  fun findOrIgnore(identifier: TopicId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asTopicId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as TopicEventG2Avro).name == DELETED

  override fun updateInternal(
      event: TopicEventG2Avro,
      currentSnapshot: Topic?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteTopic(currentSnapshot)
    } else {
      when (currentSnapshot == null) {
        true -> createTopic(event.aggregate)
        false -> {
          updateTopic(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == TOPIC.value &&
          message is TopicEventG2Avro &&
          message.name in setOf(CREATED, UPDATED, DEESCALATED, ESCALATED, DELETED)

  private fun createTopic(aggregate: TopicAggregateG2Avro) {
    val taskIdentifier = findTaskIdOrFail(aggregate.task.identifier.asTaskId())

    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("criticality", criticality.name)
          .addValue("description", description)
          .addValue("task_id", taskIdentifier)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateTopic(aggregate: TopicAggregateG2Avro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("description", description)
          .addValue("criticality", criticality.name)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteTopic(topic: Topic) {
    val topicIds = listOf(topic.id!!)
    messageDeleteService.deletePartitioned(topicIds)
    topicAttachmentService.deletePartitioned(topicIds)
    repository.delete(topic)
  }

  private fun findTaskIdOrFail(taskIdentifier: TaskId): Long? =
      taskRepository.findIdByIdentifier(taskIdentifier)

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO topic (identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "criticality, description, task_id) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":criticality, :description, :task_id);"

    private const val UPDATE_STATEMENT =
        "UPDATE topic " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "description=:description," +
            "criticality=:criticality " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
