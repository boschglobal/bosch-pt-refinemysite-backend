/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.repository

import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph

interface TopicAttachmentRepository :
    KafkaStreamableRepository<TopicAttachment, Long, TopicAttachmentEventEnumAvro>,
    TopicAttachmentRepositoryExtension {

  @EntityGraph(attributePaths = ["task.project", "topic", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): TopicAttachment?

  fun <T> findOneByIdentifier(attachmentIdentifier: UUID, type: Class<T>): T?

  fun <T> findAllByTopicIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
      topicIdentifier: TopicId,
      sort: Sort,
      type: Class<T>
  ): Collection<T>

  fun <T> findAllByTopicIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
      topicIdentifier: Collection<TopicId>,
      sort: Sort,
      type: Class<T>
  ): Collection<T>

  @EntityGraph(attributePaths = ["task", "topic"])
  fun <T> findAllByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
      taskIdentifiers: Collection<TaskId>
  ): Collection<T>
}
