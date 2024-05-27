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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.CREATED
import java.time.Instant
import java.util.UUID

@JvmOverloads
fun EventStreamGenerator.submitInvitation(
    asReference: String = "invitation",
    auditUserReference: String = DEFAULT_USER,
    eventType: InvitationEventEnumAvro = CREATED,
    time: Instant = getContext().timeLineGenerator.next(),
    businessTransactionReference: String? = getContext().lastBusinessTransactionReference,
    aggregateModifications: ((InvitationAggregateAvro.Builder) -> Unit)? = null
): EventStreamGenerator {
  val existingInvitation = get<InvitationAggregateAvro?>(asReference)

  val defaultAggregateModifications: ((InvitationAggregateAvro.Builder) -> Unit) = {
    setAuditingInformation(it.auditingInformationBuilder, eventType.name, auditUserReference, time)
    it.aggregateIdentifierBuilder.increase(eventType.name)
    it.lastSent = time.toEpochMilli()
  }

  val referenceModifications: ((InvitationAggregateAvro.Builder) -> Unit) = {
    it.projectIdentifier =
        it.projectIdentifier ?: getContext().lastIdentifierPerType[PROJECT.value]?.getIdentifier()
    it.participantIdentifier =
        it.participantIdentifier
            ?: getContext().lastIdentifierPerType[PARTICIPANT.value]?.getIdentifier()
  }

  val invitationEvent =
      existingInvitation.buildEventAvro(
          eventType, defaultAggregateModifications, aggregateModifications, referenceModifications)

  val sentEvent =
      send(
          "invitation",
          asReference,
          null,
          invitationEvent,
          time.toEpochMilli(),
          businessTransactionReference)
          as InvitationEventAvro
  getContext().events[asReference] = sentEvent.getAggregate()

  return this
}

@JvmOverloads
fun EventStreamGenerator.submitInvitationTombstones(
    reference: String = "invitation",
    messageKey: AggregateEventMessageKey? = null
): EventStreamGenerator {
  val invitation = get<InvitationAggregateAvro>(reference)!!

  if (messageKey == null) {
    val maxVersion = invitation.getAggregateIdentifier().getVersion()
    val invitationIdentifier = invitation.getAggregateIdentifier()
    for (version in 0..maxVersion) {
      val key =
          AggregateEventMessageKey(
              invitationIdentifier.buildAggregateIdentifier(version = version),
              invitation.getAggregateIdentifier().getIdentifier().toUUID())

      sendTombstoneMessage("invitation", reference, key)
    }
  } else {
    sendTombstoneMessage("invitation", reference, messageKey)
  }
  return this
}

private fun InvitationAggregateAvro?.buildEventAvro(
    eventType: InvitationEventEnumAvro,
    vararg blocks: ((InvitationAggregateAvro.Builder) -> Unit)?
): InvitationEventAvro =
    (this?.let { InvitationEventAvro.newBuilder().setName(eventType).setAggregate(this) }
            ?: newInvitation(eventType))
        .apply { for (block in blocks) block?.invoke(aggregateBuilder) }
        .build()

private fun newInvitation(event: InvitationEventEnumAvro = CREATED): InvitationEventAvro.Builder {
  val invitation =
      InvitationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              newAggregateIdentifier(ProjectmanagementAggregateTypeEnum.INVITATION.value))
          .setAuditingInformationBuilder(newAuditingInformation())
          .setEmail(UUID.randomUUID().toString() + "@example.com")
          .setLastSent(Instant.now().toEpochMilli())

  return InvitationEventAvro.newBuilder().setAggregateBuilder(invitation).setName(event)
}
