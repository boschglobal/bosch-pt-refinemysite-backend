/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.repository

import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AttachmentRepository : KafkaStreamableRepository<Attachment<*, *>, Long, Enum<*>> {

  @Query(
      "select attachment.identifier from Attachment attachment " +
          "join attachment.task task " +
          "left join attachment.topic topic " +
          "where task.identifier = :taskIdentifier " +
          "and task.deleted = false " +
          "and coalesce(topic.deleted, false) = false")
  fun findAllIdentifiersByTaskIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
      @Param("taskIdentifier") taskIdentifier: TaskId
  ): List<UUID>

  @Query(
      "select attachment.identifier from Attachment attachment " +
          "join attachment.task task " +
          "left join attachment.topic topic " +
          "where task.identifier in :taskIdentifiers " +
          "and task.deleted = false " +
          "and coalesce(topic.deleted, false) = false")
  fun findAllIdentifiersByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
      @Param("taskIdentifiers") taskIdentifiers: Collection<TaskId>,
      pageable: Pageable
  ): Slice<UUID>

  fun <T> findAllByIdentifierIn(identifiers: Collection<UUID>, sort: Sort, type: Class<T>): List<T>

  fun <T> findAllByTopicIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
      topicIdentifier: TopicId,
      sort: Sort,
      type: Class<T>
  ): List<T>

  @EntityGraph(attributePaths = ["task", "topic", "message"])
  fun findAttachmentByIdentifier(attachmentIdentifier: UUID): Attachment<*, *>?

  @Query(
      "select attachment.task.identifier from Attachment attachment " +
          "where attachment.identifier = :attachmentIdentifier")
  fun findTaskIdentifierByIdentifier(
      @Param("attachmentIdentifier") attachmentIdentifier: UUID
  ): TaskId?
}
