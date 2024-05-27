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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.ProjectReferencedAggregateTypesEnum.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.project.ProjectReferencedAggregateTypesEnum.USER
import java.time.Instant
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitParticipantG3(
    asReference: String = "participant",
    rootContextIdentifier: UUID =
        getContext().lastIdentifierPerType[PROJECT.value]!!.getIdentifier().toUUID(),
    auditUserReference: String = DEFAULT_USER,
    eventType: ParticipantEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((ParticipantAggregateG3Avro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingParticipant = get<ParticipantAggregateG3Avro?>(asReference)

  val defaultAggregateModifications: ((ParticipantAggregateG3Avro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
  }

  val referenceModifications: ((ParticipantAggregateG3Avro.Builder) -> Unit) = {
    it.project = it.project ?: getContext().lastIdentifierPerType[PROJECT.value]
    it.company = it.company ?: getContext().lastIdentifierPerType[COMPANY.value]
    it.user = it.user ?: getContext().lastIdentifierPerType[USER.value]
  }

  val participantEvent =
      existingParticipant.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val messageKey =
      AggregateEventMessageKey(
          participantEvent.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          rootContextIdentifier)

  val sentEvent =
      send(
          "project",
          asReference,
          messageKey,
          participantEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as ParticipantEventG3Avro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

private fun ParticipantAggregateG3Avro?.buildEventAvro(
    eventType: ParticipantEventEnumAvro,
    vararg blocks: ((ParticipantAggregateG3Avro.Builder) -> Unit)?
): ParticipantEventG3Avro =
    (this?.let { ParticipantEventG3Avro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newParticipantG3(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newParticipantG3(
    event: ParticipantEventEnumAvro = CREATED
): ParticipantEventG3Avro.Builder {
  val participant =
      ParticipantAggregateG3Avro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.PARTICIPANT.value))
          .setAuditingInformationBuilder(EventStreamGenerator.newAuditingInformation())
          .setRole(ParticipantRoleEnumAvro.CSM)
          .setStatus(ParticipantStatusEnumAvro.ACTIVE)

  return ParticipantEventG3Avro.newBuilder().setAggregateBuilder(participant).setName(event)
}
