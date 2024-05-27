/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.shared.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import java.util.Date
import java.util.UUID
import org.apache.commons.lang3.tuple.Pair
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TopicRepository :
    JpaRepository<Topic, Long>, TopicRepositoryExtension, JpaSpecificationExecutor<Topic> {

  @EntityGraph(attributePaths = ["task", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(topicIdentifier: TopicId): Topic?

  fun findOneByIdentifier(topicIdentifier: TopicId): Topic?

  @Query("select t.id from Topic t where t.identifier = :identifier")
  fun findIdByIdentifier(@Param("identifier") identifier: TopicId): Long?

  fun findAllByTaskIdentifierIn(identifiers: Collection<TaskId>): List<Topic>

  fun <T> findOneByIdentifier(topicIdentifier: TopicId, type: Class<T>): T?

  fun findAllByIdentifierIn(identifiers: List<TopicId>): List<Topic>

  fun <T> findAllByTaskIdentifierInAndDeletedFalse(
      identifiers: Collection<TaskId>,
      pageable: Pageable,
      type: Class<T>
  ): Slice<T>

  fun <T> findAllByTaskIdentifierAndDeletedFalseAndCreatedDateLessThanOrderByCreatedDateDesc(
      taskIdentifier: TaskId,
      createdDate: Date,
      pageable: Pageable,
      type: Class<T>
  ): Slice<T>

  @Query(
      "select new org.apache.commons.lang3.tuple.ImmutablePair( topic.identifier, count(message.identifier) ) " +
          "from Topic topic left join topic.messages message " +
          "where topic.identifier in :topicIdentifiers group by topic.identifier")
  fun findMessageCountByTopicIdentifierIn(
      @Param("topicIdentifiers") topicIdentifiers: Collection<TopicId>
  ): Collection<Pair<TopicId, Long>>

  @Query(
      "select task.identifier from Topic topic join topic.task task where topic.identifier = :topicIdentifier")
  fun findTaskIdentifierByIdentifier(@Param("topicIdentifier") topicIdentifier: TopicId): TaskId?

  @Query("select t.task.project.identifier from Topic t where t.identifier = :identifier")
  fun findProjectIdentifierByIdentifier(@Param("identifier") identifier: TopicId): ProjectId

  @Query(
      "select topic.identifier from Topic topic " +
          "join topic.task task " +
          "join task.project project " +
          "join project.participants participant " +
          "join participant.user user " +
          "where topic.identifier in :topicIdentifiers " +
          "and user.identifier = :userIdentifier " +
          "and participant.status = " +
          "com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE " +
          "and topic.createdBy.identifier = user.identifier")
  fun findAllIfUserIsCreatorOfTopicsAndActive(
      @Param("topicIdentifiers") topicIdentifiers: Set<TopicId>,
      @Param("userIdentifier") userIdentifier: UUID
  ): Set<TopicId>

  @EntityGraph(attributePaths = ["messages", "messages.createdDate", "messages.createdBy"])
  fun findAllByTaskProjectIdentifierAndDeletedFalse(projectIdentifier: ProjectId): List<Topic>
}
