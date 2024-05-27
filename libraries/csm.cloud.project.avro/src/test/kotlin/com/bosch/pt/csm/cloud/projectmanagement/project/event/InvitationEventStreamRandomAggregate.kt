/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.event

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import java.time.Instant.now
import java.util.UUID.randomUUID

@Deprecated("to be removed")
fun randomInvitation(
    block: ((InvitationAggregateAvro) -> Unit)? = null,
    event: InvitationEventEnumAvro = InvitationEventEnumAvro.CREATED
): InvitationEventAvro.Builder {
  val invitation =
      InvitationAggregateAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              defaultIdentifier(ProjectmanagementAggregateTypeEnum.INVITATION.value))
          .setAuditingInformation(randomAuditing())
          .setProjectIdentifier(randomUUID().toString())
          .setParticipantIdentifier(randomUUID().toString())
          .setEmail(randomUUID().toString() + "@example.com")
          .setLastSent(now().toEpochMilli())
          .build()
          .also { block?.invoke(it) }

  return InvitationEventAvro.newBuilder().setAggregate(invitation).setName(event)
}
