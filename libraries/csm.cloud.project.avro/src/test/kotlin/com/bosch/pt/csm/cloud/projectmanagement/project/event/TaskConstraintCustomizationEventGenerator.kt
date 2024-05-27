/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKCONSTRAINTCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTaskConstraintCustomization(
    asReference: String = "taskConstraintCustomization",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TaskConstraintCustomizationEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TaskConstraintCustomizationAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingConstraint = get<TaskConstraintCustomizationAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TaskConstraintCustomizationAggregateAvro.Builder) -> Unit) =
      {
        setAuditingInformation(
            it.auditingInformationBuilder, eventType.name, auditUserReference, time)
        it.aggregateIdentifierBuilder.increase(eventType.name)
      }

  val referenceModifications: ((TaskConstraintCustomizationAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val constraintEvent =
      existingConstraint.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          constraintEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          constraintEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TaskConstraintCustomizationEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TaskConstraintCustomizationAggregateAvro?.buildEventAvro(
    eventType: TaskConstraintCustomizationEventEnumAvro,
    vararg blocks: ((TaskConstraintCustomizationAggregateAvro.Builder) -> Unit)?
) =
    (this?.let {
          TaskConstraintCustomizationEventAvro.newBuilder().setName(eventType).setAggregate(this)
        }
            ?: newConstraint(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newConstraint(
    event: TaskConstraintCustomizationEventEnumAvro = CREATED
): TaskConstraintCustomizationEventAvro.Builder {
  val constraint =
      TaskConstraintCustomizationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(TASKCONSTRAINTCUSTOMIZATION.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setKey(MATERIAL)
          .setActive(true)

  return TaskConstraintCustomizationEventAvro.newBuilder()
      .setAggregateBuilder(constraint)
      .setName(event)
}
