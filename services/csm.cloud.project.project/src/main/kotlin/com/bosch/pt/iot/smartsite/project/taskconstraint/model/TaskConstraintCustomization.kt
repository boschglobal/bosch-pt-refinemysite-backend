/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKCONSTRAINTCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.util.UUID
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.apache.avro.specific.SpecificRecord
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = arrayOf(JoinColumn(nullable = false)),
        foreignKey = ForeignKey(name = "FK_TskConCust_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = arrayOf(JoinColumn(nullable = false)),
        foreignKey = ForeignKey(name = "FK_TskConCust_LastModifiedBy")))
@Table(
    indexes = [Index(name = "UK_TskConCust_Identifier", columnList = "identifier", unique = true)])
class TaskConstraintCustomization(

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_TskConCust_Project"))
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var project: Project,

    // key
    @field:NotNull @Column(name = "tsk_con_key", nullable = false) var key: TaskConstraintEnum,

    // active
    @field:NotNull @Column(nullable = false) var active: Boolean,

    // Name
    @field:Size(min = 1, max = MAX_NAME_LENGTH)
    @Column(nullable = true, length = MAX_NAME_LENGTH)
    var name: String?,
) :
    AbstractKafkaStreamable<
        Long, TaskConstraintCustomization, TaskConstraintCustomizationEventEnumAvro>() {

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toAvroMessage(): SpecificRecord {
    val constraintAggregateAvro =
        TaskConstraintCustomizationAggregateAvro.newBuilder()
            .setAggregateIdentifier(toAggregateIdentifier(eventType == DELETED))
            .setAuditingInformation(toAuditingInformationAvro(eventType == DELETED))
            .setProject(project.identifier.toAggregateReference())
            .setKey(TaskActionEnumAvro.valueOf(key.name))
            .setActive(active)
            .setName(name)
    return TaskConstraintCustomizationEventAvro.newBuilder()
        .setName(eventType)
        .setAggregateBuilder(constraintAggregateAvro)
        .build()
  }

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(eventType == DELETED).buildAggregateIdentifier(),
          project.identifier.toUuid())

  @ExcludeFromCodeCoverage override fun getChannel() = KafkaTopicProperties.PROJECT_BINDING

  @ExcludeFromCodeCoverage override fun getDisplayName(): String? = name ?: key.name

  @ExcludeFromCodeCoverage override fun getAggregateType() = TASKCONSTRAINTCUSTOMIZATION.value

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TaskConstraintCustomization) return false
    if (!super.equals(other)) return false

    if (project != other.project) return false
    if (key != other.key) return false
    if (active != other.active) return false
    if (name != other.name) return false

    return true
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + project.hashCode()
    result = 31 * result + key.hashCode()
    result = 31 * result + active.hashCode()
    result = 31 * result + (name?.hashCode() ?: 0)
    return result
  }

  @ExcludeFromCodeCoverage
  override fun toString(): String =
      ToStringBuilder(this)
          .apply {
            appendSuper(super.toString())
            append("identifier", identifier)
            append("key", key)
            append("name", name)
          }
          .toString()

  companion object {
    const val MAX_NAME_LENGTH = 50

    @JvmStatic
    fun newInstance() =
        TaskConstraintCustomization(Project(), TaskConstraintEnum.CUSTOM1, false, null)
  }
}
