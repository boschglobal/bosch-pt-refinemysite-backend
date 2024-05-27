/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MESSAGE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.boundary.MessageAttachmentService
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class MessageSnapshotStore(
    private val snapshotCache: MessageSnapshotEntityCache,
    private val repository: MessageRepository,
    private val topicRepository: TopicRepository,
    private val messageAttachmentService: MessageAttachmentService,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<MessageEventAvro, MessageSnapshot, Message, MessageId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: MessageId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              MESSAGE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  fun findAllOrIgnore(identifiers: List<MessageId>) =
      snapshotCache.populateFromCall { snapshotCache.loadAllFromDatabase(identifiers) }

  fun findOrIgnore(identifier: MessageId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asMessageId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as MessageEventAvro).name == DELETED

  override fun updateInternal(
      event: MessageEventAvro,
      currentSnapshot: Message?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteMessage(currentSnapshot)
    } else {
      when (currentSnapshot == null) {
        true -> createMessage(event.aggregate)
        false -> {
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == ProjectmanagementAggregateTypeEnum.MESSAGE.value &&
          message is MessageEventAvro &&
          message.name in setOf(CREATED, DELETED)

  private fun createMessage(aggregate: MessageAggregateAvro) {
    val topicIdentifier = findTopicIdOrFail(aggregate.topic.identifier.asTopicId())

    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("content", content)
          .addValue("topic_id", topicIdentifier)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun deleteMessage(message: Message) {
    messageAttachmentService.deletePartitioned(listOf(message.id!!))
    repository.delete(message)
  }

  private fun findTopicIdOrFail(topicIdentifier: TopicId): Long? =
      topicRepository.findIdByIdentifier(topicIdentifier)

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO message (identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "content, topic_id) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":content, :topic_id);"
  }
}
