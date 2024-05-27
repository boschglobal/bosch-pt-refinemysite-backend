/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.repository

import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph

interface MessageAttachmentRepository :
    KafkaStreamableRepository<MessageAttachment, Long, MessageAttachmentEventEnumAvro>,
    MessageAttachmentRepositoryExtension {

  @EntityGraph(attributePaths = ["task.project", "topic", "message", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): MessageAttachment?

  fun <T> findOneByIdentifier(attachmentIdentifier: UUID, type: Class<T>): T?

  fun <T> findAllByMessageIdentifierAndTaskDeletedFalseAndTopicDeletedFalse(
      messageIdentifier: MessageId,
      sort: Sort,
      type: Class<T>
  ): Collection<T>

  fun <T> findAllByMessageIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
      messageIdentifiers: Collection<MessageId>,
      sort: Sort,
      type: Class<T>
  ): Collection<T>

  @EntityGraph(attributePaths = ["task", "topic"])
  fun <T> findAllByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse(
      taskIdentifiers: Collection<TaskId>
  ): Collection<T>
}
