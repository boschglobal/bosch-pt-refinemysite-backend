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
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitProjectPicture(
    asReference: String = "projectPicture",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: ProjectPictureEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ProjectPictureAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingProjectPicture = get<ProjectPictureAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((ProjectPictureAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((ProjectPictureAggregateAvro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
  }

  val projectPictureEvent =
      existingProjectPicture.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          projectPictureEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          projectPictureEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ProjectPictureEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun ProjectPictureAggregateAvro?.buildEventAvro(
    eventType: ProjectPictureEventEnumAvro,
    vararg blocks: ((ProjectPictureAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { ProjectPictureEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newProjectCraft(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newProjectCraft(
    event: ProjectPictureEventEnumAvro = CREATED
): ProjectPictureEventAvro.Builder {
  val projectPicture =
      ProjectPictureAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.PROJECTPICTURE.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setFileSize(1000)
          .setFullAvailable(false)
          .setSmallAvailable(false)
          .setWidth(768)
          .setHeight(1024)

  return ProjectPictureEventAvro.newBuilder().setAggregateBuilder(projectPicture).setName(event)
}
