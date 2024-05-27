/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreTaskAttachmentStrategy(
    private val taskRepository: TaskRepository,
    private val taskAttachmentRepository: TaskAttachmentRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, taskAttachmentRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      TASKATTACHMENT.value == record.key().aggregateIdentifier.type &&
          record.value() is TaskAttachmentEventAvro?

  public override fun doHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ) {
    val key = record.key()
    val event = record.value() as TaskAttachmentEventAvro?
    assertEventNotNull(event, key)

    if (event!!.name == DELETED) {
      deleteTaskAttachment(key.aggregateIdentifier.identifier)
    } else if (event.name == CREATED || event.name == UPDATED) {
      val aggregate = event.aggregate
      val taskAttachment = findTaskAttachment(aggregate.aggregateIdentifier)
      if (taskAttachment == null) {
        createTaskAttachment(aggregate)
      } else {
        updateTaskAttachment(taskAttachment, aggregate)
      }
    } else {
      handleInvalidEventType(event.name.name)
    }
  }

  private fun createTaskAttachment(aggregate: TaskAttachmentAggregateAvro) {
    entityManager.persist(
        TaskAttachment().apply {
          setTaskAttachmentAttributes(this, aggregate)
          setAuditAttributes(this, aggregate.auditingInformation)
        })
  }

  private fun updateTaskAttachment(
      taskAttachment: TaskAttachment,
      aggregate: TaskAttachmentAggregateAvro
  ) {
    update(
        taskAttachment,
        object : DetachedEntityUpdateCallback<TaskAttachment> {
          override fun update(entity: TaskAttachment) {
            setTaskAttachmentAttributes(entity, aggregate)
            setAuditAttributes(entity, aggregate.auditingInformation)
          }
        })
  }

  private fun deleteTaskAttachment(identifier: UUID) {
    delete(taskAttachmentRepository.findOneWithDetailsByIdentifier(identifier))
  }

  private fun setTaskAttachmentAttributes(
      taskAttachment: TaskAttachment,
      aggregate: TaskAttachmentAggregateAvro
  ) {
    val attachmentAvro = aggregate.attachment
    taskAttachment.identifier = UUID.fromString(aggregate.aggregateIdentifier.identifier)
    taskAttachment.version = aggregate.aggregateIdentifier.version
    taskAttachment.task = findTaskOrFail(aggregate.task)
    taskAttachment.fileName = attachmentAvro.fileName
    taskAttachment.fileSize = attachmentAvro.fileSize
    taskAttachment.imageHeight = attachmentAvro.height
    taskAttachment.imageWidth = attachmentAvro.width
    taskAttachment.setFullAvailable(attachmentAvro.fullAvailable)
    taskAttachment.setSmallAvailable(attachmentAvro.smallAvailable)
    taskAttachment.captureDate =
        if (attachmentAvro.captureDate == null) null else Date(attachmentAvro.captureDate)
  }

  private fun findTaskAttachment(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ): TaskAttachment? =
      taskAttachmentRepository.findOneWithDetailsByIdentifier(
          UUID.fromString(aggregateIdentifierAvro.identifier))

  private fun findTaskOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Task =
      requireNotNull(
          taskRepository.findOneByIdentifier(aggregateIdentifierAvro.identifier.asTaskId())) {
            "Task missing: ${aggregateIdentifierAvro.identifier}"
          }
}
