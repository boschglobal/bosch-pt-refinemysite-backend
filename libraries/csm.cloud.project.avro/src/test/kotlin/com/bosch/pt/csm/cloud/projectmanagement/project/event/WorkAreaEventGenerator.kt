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
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitWorkArea(
    asReference: String = "workArea",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: WorkAreaEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((WorkAreaAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingWorkArea = get<WorkAreaAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((WorkAreaAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val workAreaEvent =
      existingWorkArea.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications)

  val messageKey =
      AggregateEventMessageKey(
          workAreaEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          workAreaEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as WorkAreaEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun WorkAreaAggregateAvro?.buildEventAvro(
    eventType: WorkAreaEventEnumAvro,
    vararg blocks: ((WorkAreaAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { WorkAreaEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newWorkArea(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newWorkArea(event: WorkAreaEventEnumAvro = CREATED): WorkAreaEventAvro.Builder {
  val workArea =
      WorkAreaAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.WORKAREA.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setName(randomString())

  return WorkAreaEventAvro.newBuilder().setAggregateBuilder(workArea).setName(event)
}
