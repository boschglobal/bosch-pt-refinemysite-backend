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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitWorkAreaList(
    asReference: String = "workAreaList",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: WorkAreaListEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((WorkAreaListAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingWorkAreaList = get<WorkAreaListAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((WorkAreaListAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((WorkAreaListAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
    if (it.workAreas == null && getContext().lastIdentifierPerType[WORKAREA.value] != null) {
      it.workAreas = listOf(getContext().lastIdentifierPerType[WORKAREA.value])
    }
  }

  val workAreaListEvent =
      existingWorkAreaList.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          workAreaListEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          workAreaListEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as WorkAreaListEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun WorkAreaListAggregateAvro?.buildEventAvro(
    eventType: WorkAreaListEventEnumAvro,
    vararg blocks: ((WorkAreaListAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { WorkAreaListEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newWorkAreaList(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newWorkAreaList(
    event: WorkAreaListEventEnumAvro = CREATED
): WorkAreaListEventAvro.Builder {
  val workAreaList =
      WorkAreaListAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREALIST.value))
          .setAuditingInformationBuilder(newAuditingInformation())

  return WorkAreaListEventAvro.newBuilder().setAggregateBuilder(workAreaList).setName(event)
}
