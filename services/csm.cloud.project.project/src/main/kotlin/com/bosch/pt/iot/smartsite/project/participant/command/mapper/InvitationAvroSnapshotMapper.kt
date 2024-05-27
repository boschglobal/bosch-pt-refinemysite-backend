/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.InvitationSnapshot

object InvitationAvroSnapshotMapper : AbstractAvroSnapshotMapper<InvitationSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: InvitationSnapshot,
      eventType: E
  ) =
      InvitationEventAvro.newBuilder()
          .setName((eventType as InvitationEventEnumAvro))
          .setAggregateBuilder(
              InvitationAggregateAvro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setEmail(snapshot.email)
                  .setLastSent(snapshot.lastSent.toEpochMilli())
                  .setProjectIdentifier(snapshot.projectRef.toString())
                  .setParticipantIdentifier(snapshot.participantRef.toString()))
          .build()

  override fun getAggregateType() = ProjectmanagementAggregateTypeEnum.INVITATION.value

  override fun getRootContextIdentifier(snapshot: InvitationSnapshot) = snapshot.identifier.toUuid()
}
