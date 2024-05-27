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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTaskAttachment(
    asReference: String = "taskAttachment",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TaskAttachmentEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TaskAttachmentAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingAttachment = get<TaskAttachmentAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TaskAttachmentAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TaskAttachmentAggregateAvro.Builder) -> Unit) = {
    it.task = it.task ?: getContext().lastIdentifierPerType[TASK.value]
  }

  val attachmentEvent =
      existingAttachment.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          attachmentEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          attachmentEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TaskAttachmentEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TaskAttachmentAggregateAvro?.buildEventAvro(
    eventType: TaskAttachmentEventEnumAvro,
    vararg blocks: ((TaskAttachmentAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { TaskAttachmentEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newAttachment(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newAttachment(
    event: TaskAttachmentEventEnumAvro = CREATED
): TaskAttachmentEventAvro.Builder {
  val attachment =
      TaskAttachmentAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.TASKATTACHMENT.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setAttachmentBuilder(
              AttachmentAvro.newBuilder()
                  .setCaptureDate(Instant.now().toEpochMilli())
                  .setFileName("myPicture.jpg")
                  .setFileSize(1000)
                  .setFullAvailable(false)
                  .setSmallAvailable(false)
                  .setWidth(768)
                  .setHeight(1024))

  return TaskAttachmentEventAvro.newBuilder().setAggregateBuilder(attachment).setName(event)
}
