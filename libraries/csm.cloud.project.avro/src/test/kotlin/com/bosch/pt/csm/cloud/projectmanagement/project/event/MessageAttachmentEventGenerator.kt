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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGEATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitMessageAttachment(
    asReference: String = "messageAttachment",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: MessageAttachmentEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((MessageAttachmentAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingAttachment = get<MessageAttachmentAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((MessageAttachmentAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((MessageAttachmentAggregateAvro.Builder) -> Unit) = {
    it.message = it.message ?: getContext().lastIdentifierPerType[MESSAGE.value]
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
          as MessageAttachmentEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun MessageAttachmentAggregateAvro?.buildEventAvro(
    eventType: MessageAttachmentEventEnumAvro,
    vararg blocks: ((MessageAttachmentAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { MessageAttachmentEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newAttachment(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newAttachment(
    event: MessageAttachmentEventEnumAvro = CREATED
): MessageAttachmentEventAvro.Builder {
  val attachment =
      MessageAttachmentAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(newAggregateIdentifier(MESSAGEATTACHMENT.value))
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

  return MessageAttachmentEventAvro.newBuilder().setAggregateBuilder(attachment).setName(event)
}
