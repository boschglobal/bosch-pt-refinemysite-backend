/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.message.shared.repository

import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.util.Date
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MessageRepository :
    JpaRepository<Message, Long>, MessageRepositoryExtension, JpaSpecificationExecutor<Message> {

  @EntityGraph(attributePaths = ["topic.task", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: MessageId): Message?

  fun findOneByIdentifier(messageIdentifier: MessageId): Message?

  fun findAllByIdentifierIn(identifiers: List<MessageId>): List<Message>

  @Query("select m from Message m where m.topic.task.identifier in :identifiers")
  fun findAllMessagesByTaskIdentifiers(
      @Param("identifiers") identifiers: Collection<TaskId>
  ): List<Message>

  @Query("select m.topic.task.project.identifier from Message m where m.identifier = :identifier")
  fun findProjectIdentifierByIdentifier(@Param("identifier") identifier: MessageId): ProjectId

  fun <T> findOneByIdentifier(messageIdentifier: MessageId, type: Class<T>): T?

  fun <T> findAllByTopicIdentifierOrderByCreatedDateDesc(
      topicIdentifier: TopicId,
      pageable: Pageable,
      type: Class<T>
  ): Slice<T>

  fun <T> findByTopicTaskIdentifierIn(
      identifiers: Collection<TaskId>,
      pageable: Pageable,
      type: Class<T>
  ): Slice<T>

  @EntityGraph(attributePaths = ["topic.task"])
  fun findAllWithDetailsByTopicTaskIdentifierIn(
      identifiers: Collection<TaskId>,
      pageable: Pageable
  ): Slice<Message>

  fun <T> findAllByTopicIdentifierAndCreatedDateLessThanOrderByCreatedDateDesc(
      topicIdentifier: TopicId,
      createdDate: Date,
      pageable: Pageable,
      type: Class<T>
  ): Slice<T>

  @Query(
      "select task.identifier from Message message join message.topic topic " +
          "join topic.task task where message.identifier = :messageIdentifier")
  fun findTaskIdentifierByIdentifier(
      @Param("messageIdentifier") messageIdentifier: MessageId
  ): TaskId?

  @Query(
      "select message.identifier.identifier from Message message, User user " +
          "where user.identifier = :userIdentifier " +
          "and message.identifier.identifier in :messageIdentifiers " +
          "and message.createdBy.identifier = user.identifier")
  fun findAllIfUserIsCreatorOfMessages(
      @Param("messageIdentifiers") messageIdentifiers: Set<UUID>,
      @Param("userIdentifier") userIdentifier: UUID
  ): Set<UUID>
}
