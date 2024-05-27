/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.PROJECT_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
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
        foreignKey = ForeignKey(name = "FK_RfvCust_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = arrayOf(JoinColumn(nullable = false)),
        foreignKey = ForeignKey(name = "FK_RfvCust_LastModifiedBy")))
@Table(indexes = [Index(name = "UK_RfvCust_Identifier", columnList = "identifier", unique = true)])
class RfvCustomization(

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_RfvCust_Project"))
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var project: Project,

    // key
    @field:NotNull @Column(name = "rfv_key", nullable = false) var key: DayCardReasonEnum,

    // active
    @field:NotNull @Column(nullable = false) var active: Boolean,

    // Name
    @field:Size(min = 1, max = MAX_NAME_LENGTH)
    @Column(nullable = true, length = MAX_NAME_LENGTH)
    var name: String?,
) : AbstractKafkaStreamable<Long, RfvCustomization, RfvCustomizationEventEnumAvro>() {

  override fun toEvent(key: ByteArray, payload: ByteArray?, partition: Int, transactionId: UUID?) =
      ProjectContextKafkaEvent(key, payload, partition, transactionId)

  override fun toAvroMessage(): SpecificRecord {
    val rfvAggregateAvro =
        RfvCustomizationAggregateAvro.newBuilder()
            .setAggregateIdentifier(toAggregateIdentifier(eventType == DELETED))
            .setAuditingInformation(toAuditingInformationAvro(eventType == DELETED))
            .setProject(project.identifier.toAggregateReference())
            .setKey(DayCardReasonNotDoneEnumAvro.valueOf(key.name))
            .setActive(active)
            .setName(name)
    return RfvCustomizationEventAvro.newBuilder()
        .setName(eventType)
        .setAggregateBuilder(rfvAggregateAvro)
        .build()
  }

  override fun toMessageKey(): AggregateEventMessageKey =
      AggregateEventMessageKey(
          toAggregateIdentifier(eventType == DELETED).buildAggregateIdentifier(),
          project.identifier.toUuid())

  @ExcludeFromCodeCoverage override fun getChannel() = PROJECT_BINDING

  @ExcludeFromCodeCoverage override fun getDisplayName(): String = name ?: key.name

  @ExcludeFromCodeCoverage override fun getAggregateType() = RFVCUSTOMIZATION.value

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RfvCustomization) return false
    if (!super.equals(other)) return false

    if (project != other.project) return false
    if (key != other.key) return false
    if (active != other.active) return false
    return name == other.name
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
            append("project", project)
            append("key", key)
            append("active", active)
            append("name", name)
          }
          .toString()

  companion object {
    const val MAX_NAME_LENGTH = 50

    @JvmStatic
    fun newInstance() = RfvCustomization(Project(), DayCardReasonEnum.CUSTOM1, false, null)
  }
}
