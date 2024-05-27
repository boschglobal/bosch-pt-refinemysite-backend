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
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitProjectCraftG2(
    asReference: String = "projectCraft",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: ProjectCraftEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ProjectCraftAggregateG2Avro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingProjectCraft = get<ProjectCraftAggregateG2Avro?>(asReference)

  val defaultAggregateModifications: ((ProjectCraftAggregateG2Avro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((ProjectCraftAggregateG2Avro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val projectCraftEvent =
      existingProjectCraft.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          projectCraftEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          projectCraftEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ProjectCraftEventG2Avro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun ProjectCraftAggregateG2Avro?.buildEventAvro(
    eventType: ProjectCraftEventEnumAvro,
    vararg blocks: ((ProjectCraftAggregateG2Avro.Builder) -> Unit)?
) =
    (this?.let { ProjectCraftEventG2Avro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newProjectCraft(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProjectCraft(
    event: ProjectCraftEventEnumAvro = CREATED
): ProjectCraftEventG2Avro.Builder {
  val projectCraft =
      ProjectCraftAggregateG2Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.PROJECTCRAFT.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setColor("#AABBCC")
          .setName(randomString())

  return ProjectCraftEventG2Avro.newBuilder().setAggregateBuilder(projectCraft).setName(event)
}
