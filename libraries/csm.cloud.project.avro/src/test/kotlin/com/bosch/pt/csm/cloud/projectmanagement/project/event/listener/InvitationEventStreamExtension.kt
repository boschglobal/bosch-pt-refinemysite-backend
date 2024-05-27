/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.randomInvitation
import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment

fun InvitationEventListener.submitInvitation(
    existingInvitation: InvitationAggregateAvro? = null,
    event: InvitationEventEnumAvro = InvitationEventEnumAvro.CREATED,
    vararg blocks: ((InvitationAggregateAvro) -> Unit)?
): InvitationAggregateAvro {
  val invitation = existingInvitation.buildEventAvro(event, *blocks)
  val key =
      AggregateEventMessageKey(
          invitation.getAggregate().getAggregateIdentifier().buildAggregateIdentifier(),
          invitation.getAggregate().getProjectIdentifier().toUUID())

  return (submitEvent(key, invitation, ::listenToInvitationEvents).value() as InvitationEventAvro)
      .getAggregate()
}

fun InvitationEventListener.submitInvitationTombstoneMessage(messageKey: AggregateEventMessageKey) {
  submitEvent(messageKey, null, ::listenToInvitationEvents)
}

@Suppress("unused")
fun <K : EventMessageKey, V : SpecificRecordBase?> InvitationEventListener.submitEvent(
    key: K,
    value: V,
    listener: (ConsumerRecord<K, V>, Acknowledgment) -> Unit
): ConsumerRecord<K, V> {
  val consumerRecord = ConsumerRecord("", 0, 0, key, value)
  mockk<Acknowledgment>(relaxed = true).apply {
    listener(consumerRecord, this)
    verify { acknowledge() }
  }
  return consumerRecord
}

private fun InvitationAggregateAvro?.buildEventAvro(
    event: InvitationEventEnumAvro,
    vararg blocks: ((InvitationAggregateAvro) -> Unit)?
) =
    (this?.let { InvitationEventAvro(event, this) } ?: randomInvitation(null, event).build())
        .apply { for (block in blocks) block?.invoke(getAggregate()) }
