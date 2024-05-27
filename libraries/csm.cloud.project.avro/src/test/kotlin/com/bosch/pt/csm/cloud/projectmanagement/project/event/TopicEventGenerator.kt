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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import java.time.Instant
import java.util.UUID
import kotlin.collections.set

@JvmOverloads
fun EventStreamGenerator.submitTopicG2(
    asReference: String = "topic",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: TopicEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((TopicAggregateG2Avro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingTopic = get<TopicAggregateG2Avro?>(asReference)

  val defaultAggregateModifications: ((TopicAggregateG2Avro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((TopicAggregateG2Avro.Builder) -> Unit) = {
    it.task = it.task ?: getContext().lastIdentifierPerType[TASK.value]
  }

  val topicEvent =
      existingTopic.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          topicEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          topicEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as TopicEventG2Avro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun TopicAggregateG2Avro?.buildEventAvro(
    eventType: TopicEventEnumAvro,
    vararg blocks: ((TopicAggregateG2Avro.Builder) -> Unit)?
) =
    (this?.let { TopicEventG2Avro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newTopic(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newTopic(event: TopicEventEnumAvro = CREATED): TopicEventG2Avro.Builder {
  val topic =
      TopicAggregateG2Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.TOPIC.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setDescription(randomString())
          .setCriticality(TopicCriticalityEnumAvro.UNCRITICAL)

  return TopicEventG2Avro.newBuilder().setAggregateBuilder(topic).setName(event)
}
