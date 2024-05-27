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
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.InvitationEventListener
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitInvitation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.submitInvitationTombstoneMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import java.time.Instant
import java.time.Instant.now
import org.apache.avro.specific.SpecificRecordBase

@Deprecated("to be removed")
class InvitationEventStreamGenerator(
    private val timeLineGenerator: TimeLineGenerator,
    private val eventListener: InvitationEventListener,
    private val context: MutableMap<String, SpecificRecordBase>
) : AbstractEventStreamGenerator(context) {

  fun submitInvitation(
      name: String = "invitation",
      userName: String = "user",
      project: String = "project",
      participant: String = "participant",
      email: String = "mustermann@example.com",
      lastSent: Instant = now(),
      eventName: InvitationEventEnumAvro = InvitationEventEnumAvro.CREATED,
      time: Instant = timeLineGenerator.next(),
      aggregateModifications: ((InvitationAggregateAvro) -> Unit)? = null
  ): InvitationEventStreamGenerator {
    val invitation = get<InvitationAggregateAvro?>(name)
    val project = get<ProjectAggregateAvro>(project)
    val participant = get<ParticipantAggregateG3Avro>(participant)

    val defaultAggregateModification: ((InvitationAggregateAvro) -> Unit) = {
      setAuditingInformationAndIncreaseVersion(it, eventName.name, userName, time)

      it.apply {
        setProjectIdentifier(project.getIdentifier().toString())
        setParticipantIdentifier(participant.getIdentifier().toString())
        setEmail(email)
        setLastSent(lastSent.toEpochMilli())
      }
    }

    context[name] =
        eventListener.submitInvitation(
            invitation, eventName, defaultAggregateModification, aggregateModifications)
    return this
  }

  fun submitInvitationTombstone(
      name: String = "invitation",
      messageKey: AggregateEventMessageKey? = null
  ): InvitationEventStreamGenerator {
    val invitation = get<InvitationAggregateAvro>(name)
    val key =
        messageKey
            ?: AggregateEventMessageKey(
                invitation.getAggregateIdentifier().buildAggregateIdentifier(),
                invitation.getAggregateIdentifier().getIdentifier().toUUID())
    eventListener.submitInvitationTombstoneMessage(key)
    return this
  }
}
