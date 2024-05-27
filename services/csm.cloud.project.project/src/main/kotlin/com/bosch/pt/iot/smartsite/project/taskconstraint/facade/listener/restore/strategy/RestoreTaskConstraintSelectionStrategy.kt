/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKACTION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintSelectionRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreTaskConstraintSelectionStrategy(
    private val taskRepository: TaskRepository,
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, taskConstraintSelectionRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      TASKACTION.value == record.key().aggregateIdentifier.type &&
          record.value() is TaskActionSelectionEventAvro?

  public override fun doHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ) {
    val key = record.key()
    val event = record.value() as TaskActionSelectionEventAvro?
    assertEventNotNull(event, key)

    if (event!!.name == DELETED) {
      deleteConstraintSelection(key.aggregateIdentifier.identifier)
    } else if (event.name == CREATED || event.name == UPDATED) {
      val aggregate = event.aggregate
      val constraintSelection = findConstraintSelection(aggregate.aggregateIdentifier)
      if (constraintSelection == null) {
        createConstraintSelection(aggregate)
      } else {
        updateConstraintSelection(constraintSelection, aggregate)
      }
    } else {
      handleInvalidEventType(event.name.name)
    }
  }

  private fun createConstraintSelection(aggregate: TaskActionSelectionAggregateAvro) {
    val constraintSelection = TaskConstraintSelection.newInstance()
    setConstraintSelectionAttributes(constraintSelection, aggregate)
    setAuditAttributes(constraintSelection, aggregate.auditingInformation)
    entityManager.persist(constraintSelection)
  }

  private fun updateConstraintSelection(
      constraintSelection: TaskConstraintSelection,
      aggregate: TaskActionSelectionAggregateAvro
  ) =
      update(
          constraintSelection,
          object : DetachedEntityUpdateCallback<TaskConstraintSelection> {
            override fun update(entity: TaskConstraintSelection) {
              setConstraintSelectionAttributes(entity, aggregate)
              setAuditAttributes(entity, aggregate.auditingInformation)
            }
          })

  private fun deleteConstraintSelection(identifier: UUID) =
      delete<TaskConstraintSelection>(
          taskConstraintSelectionRepository.findOneWithDetailsByIdentifier(identifier))

  private fun setConstraintSelectionAttributes(
      constraintSelection: TaskConstraintSelection,
      aggregate: TaskActionSelectionAggregateAvro
  ) {
    constraintSelection.identifier = aggregate.aggregateIdentifier.identifier.toUUID()
    constraintSelection.version = aggregate.aggregateIdentifier.version
    constraintSelection.task = findTask(aggregate.task)
    constraintSelection.constraints.clear()
    constraintSelection.constraints.addAll(
        aggregate.actions.map { TaskConstraintEnum.valueOf(it.name) })
  }

  private fun findConstraintSelection(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ): TaskConstraintSelection? =
      taskConstraintSelectionRepository.findOneWithDetailsByIdentifier(
          aggregateIdentifierAvro.identifier.toUUID())

  private fun findTask(aggregateIdentifierAvro: AggregateIdentifierAvro): Task =
      checkNotNull(
          taskRepository.findOneByIdentifier(aggregateIdentifierAvro.identifier.asTaskId())) {
            "Task missing: ${aggregateIdentifierAvro.identifier}"
          }
}
