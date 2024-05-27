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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.messages.AttachmentAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTopicAttachment(
    asReference: String = "topicAttachment",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TopicAttachmentEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TopicAttachmentAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingAttachment = get<TopicAttachmentAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((TopicAttachmentAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TopicAttachmentAggregateAvro.Builder) -> Unit) = {
    it.topic = it.topic ?: getContext().lastIdentifierPerType[TOPIC.value]
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
          as TopicAttachmentEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TopicAttachmentAggregateAvro?.buildEventAvro(
    eventType: TopicAttachmentEventEnumAvro,
    vararg blocks: ((TopicAttachmentAggregateAvro.Builder) -> Unit)?
) =
    (this?.let { TopicAttachmentEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newAttachment(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newAttachment(
    event: TopicAttachmentEventEnumAvro = CREATED
): TopicAttachmentEventAvro.Builder {
  val attachment =
      TopicAttachmentAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.TOPICATTACHMENT.value))
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

  return TopicAttachmentEventAvro.newBuilder().setAggregateBuilder(attachment).setName(event)
}
