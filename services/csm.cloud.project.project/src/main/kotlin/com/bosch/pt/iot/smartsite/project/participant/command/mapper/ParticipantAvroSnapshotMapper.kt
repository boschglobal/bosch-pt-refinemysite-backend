/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.mapper

import com.bosch.pt.csm.cloud.common.api.toAggregateReference
import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.iot.smartsite.company.api.toAggregateReference
import com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore.ParticipantSnapshot
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference

object ParticipantAvroSnapshotMapper : AbstractAvroSnapshotMapper<ParticipantSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: ParticipantSnapshot,
      eventType: E
  ) =
      ParticipantEventG3Avro.newBuilder()
          .setName((eventType as ParticipantEventEnumAvro))
          .setAggregateBuilder(
              ParticipantAggregateG3Avro.newBuilder()
                  .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                  .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                  .setProject(snapshot.projectRef.toAggregateReference())
                  .setCompany(snapshot.companyRef?.toAggregateReference())
                  .setUser(snapshot.userRef?.toAggregateReference())
                  .setRole(ParticipantRoleEnumAvro.valueOf(snapshot.role.name))
                  .setStatus(ParticipantStatusEnumAvro.valueOf(snapshot.status.toString())))
          .build()

  override fun getAggregateType() = ProjectmanagementAggregateTypeEnum.PARTICIPANT.value

  override fun getRootContextIdentifier(snapshot: ParticipantSnapshot) =
      snapshot.projectRef.toUuid()
}
