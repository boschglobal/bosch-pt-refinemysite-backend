/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.DEFAULT_USER
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator.Companion.newAuditingInformation
import com.bosch.pt.csm.cloud.common.test.randomLong
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.CREATED
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitProject(
    asReference: String = "project",
    auditUserReference: String = DEFAULT_USER,
    eventType: ProjectEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ProjectAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingProject = get<ProjectAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((ProjectAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val projectEvent =
      existingProject.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications)

  val sentEvent =
      send(
          "project",
          asReference,
          null,
          projectEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ProjectEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()
  getContext().lastRootContextIdentifier = sentEvent.getAggregate().getAggregateIdentifier()

  return this
}

private fun ProjectAggregateAvro?.buildEventAvro(
    eventType: ProjectEventEnumAvro,
    vararg blocks: ((ProjectAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { ProjectEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newProject(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProject(event: ProjectEventEnumAvro = CREATED): ProjectEventAvro.Builder {
  val project =
      ProjectAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.PROJECT.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setStart(
              LocalDate.now()
                  .plusDays(randomLong(0L, 30L))
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli())
          .setEnd(
              LocalDate.now()
                  .plusDays(randomLong(30L, 60L))
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli())
          .setProjectNumber(randomString())
          .setTitle(randomString())
          .setProjectAddress(randomProjectAddress())

  return ProjectEventAvro.newBuilder().setAggregateBuilder(project).setName(event)
}
