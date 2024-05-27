/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskconstraint.model

import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKACTION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID
import org.apache.avro.specific.SpecificRecord

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_TaskActionSelection_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_TaskActionSelection_LastModifiedBy")))
@Table(
    name = "task_action_selection",
    indexes =
        [
            Index(
                name = "UK_TaskActionSelection_Identifier",
                columnList = "identifier",
                unique = true),
            Index(name = "UK_TaskActionSelection_Task", columnList = "task_id", unique = true)])
data class TaskConstraintSelection(

    // Task
    @OneToOne(optional = false)
    @JoinColumn(nullable = false, foreignKey = ForeignKey(name = "FK_TaskActionSelection_Task"))
    var task: Task,

    // Constraints
    @ElementCollection(fetch = EAGER, targetClass = TaskConstraintEnum::class)
    @CollectionTable(
        name = "task_action_selection_set",
        joinColumns = [JoinColumn(name = "task_action_selection_id")],
        foreignKey = ForeignKey(name = "FK_TaskActionSelectionSet_TaskActionSelection"))
    @Column(name = "action", nullable = false, columnDefinition = "varchar(255)")
    @Enumerated(EnumType.STRING)
    val constraints: MutableSet<TaskConstraintEnum> = mutableSetOf()
) : AbstractKafkaStreamable<Long, TaskConstraintSelection, TaskActionSelectionEventEnumAvro>() {

  override fun getDisplayName(): String? = identifier.toString()

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(DELETED == eventType).buildAggregateIdentifier(),
          task.project!!.identifier.toUuid())

  override fun toAvroMessage(): SpecificRecord =
      TaskActionSelectionEventAvro.newBuilder()
          .setName(eventType)
          .setAggregateBuilder(
              TaskActionSelectionAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifier(DELETED == eventType))
                  .setAuditingInformation(toAuditingInformationAvro(DELETED == eventType))
                  .setTask(task.identifier.toAggregateReference())
                  .setActions(constraints.map { TaskActionEnumAvro.valueOf(it.name) }))
          .build()

  override fun getChannel(): String = PROJECT_BINDING

  override fun getAggregateType(): String = TASKACTION.value

  companion object {
    private const val serialVersionUID: Long = 676923527778489282

    @JvmStatic fun newInstance() = TaskConstraintSelection(Task())
  }
}
