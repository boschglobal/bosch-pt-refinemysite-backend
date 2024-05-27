/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFTLIST
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitProjectCraftList(
    asReference: String = "projectCraftList",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.identifier.toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: ProjectCraftListEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ProjectCraftListAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {

  val existingProjectCraftList = get<ProjectCraftListAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((ProjectCraftListAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((ProjectCraftListAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
    if (it.projectCrafts == null &&
        getContext().lastIdentifierPerType[PROJECTCRAFT.value] != null) {
      it.projectCrafts = listOf(getContext().lastIdentifierPerType[PROJECTCRAFT.value])
    }
  }

  val projectCraftListEvent =
      existingProjectCraftList.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          projectCraftListEvent.aggregate.aggregateIdentifier.buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          projectCraftListEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ProjectCraftListEventAvro
  getContext().events[asReference] = sentEvent.aggregate

  return this
}

private fun ProjectCraftListAggregateAvro?.buildEventAvro(
    eventType: ProjectCraftListEventEnumAvro,
    vararg blocks: ((ProjectCraftListAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { ProjectCraftListEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newProjectCraftList(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProjectCraftList(
    event: ProjectCraftListEventEnumAvro = CREATED
): ProjectCraftListEventAvro.Builder {
  val projectCraftList =
      ProjectCraftListAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(PROJECTCRAFTLIST.value))
          .setAuditingInformationBuilder(newAuditingInformation())

  return ProjectCraftListEventAvro.newBuilder().setAggregateBuilder(projectCraftList).setName(event)
}
