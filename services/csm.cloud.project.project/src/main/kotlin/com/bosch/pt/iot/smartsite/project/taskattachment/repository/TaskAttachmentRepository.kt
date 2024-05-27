/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.repository

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import java.util.UUID
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TaskAttachmentRepository :
    KafkaStreamableRepository<TaskAttachment, Long, TaskAttachmentEventEnumAvro>,
    TaskAttachmentRepositoryExtension {

  @EntityGraph(attributePaths = ["task.project", "message", "topic", "createdBy", "lastModifiedBy"])
  fun findOneWithDetailsByIdentifier(identifier: UUID): TaskAttachment?

  fun <T> findOneByIdentifier(attachmentIdentifier: UUID, type: Class<T>): T?

  fun <T> findAllByTaskIdentifierAndTaskDeletedFalse(
      taskIdentifiers: TaskId,
      sort: Sort,
      type: Class<T>
  ): List<T>

  fun <T> findAllByTaskIdentifierInAndTaskDeletedFalse(
      taskIdentifiers: Collection<TaskId>,
      sort: Sort,
      type: Class<T>
  ): List<T>

  @Query(
      "select attachment.task.identifier from Attachment attachment " +
          "where attachment.identifier = :attachmentIdentifier " +
          "and attachment.task is not null " +
          "and attachment.topic is null " +
          "and attachment.message is null")
  fun findTaskIdentifierFromTaskAttachment(
      @Param("attachmentIdentifier") attachmentIdentifier: UUID
  ): TaskId?
}
