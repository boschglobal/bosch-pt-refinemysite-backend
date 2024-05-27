/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneTypeEnumAvro
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneSnapshot
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.workarea.domain.toAggregateReference

object MilestoneAvroSnapshotMapper : AbstractAvroSnapshotMapper<MilestoneSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: MilestoneSnapshot,
      eventType: E
  ): MilestoneEventAvro =
      with(snapshot) {
        MilestoneEventAvro.newBuilder()
            .setName(eventType as MilestoneEventEnumAvro)
            .setAggregateBuilder(
                MilestoneAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setName(name)
                    .setType(MilestoneTypeEnumAvro.valueOf(type.name))
                    .setDate(date.toEpochMilli())
                    .setHeader(header)
                    .setProject(projectRef.toAggregateReference())
                    .setCraft(craftRef?.toAggregateReference())
                    .setWorkarea(workAreaRef?.toAggregateReference())
                    .setDescription(description))
            .build()
      }

  override fun getAggregateType() = MILESTONE.value

  override fun getRootContextIdentifier(snapshot: MilestoneSnapshot) = snapshot.projectRef.toUuid()
}
