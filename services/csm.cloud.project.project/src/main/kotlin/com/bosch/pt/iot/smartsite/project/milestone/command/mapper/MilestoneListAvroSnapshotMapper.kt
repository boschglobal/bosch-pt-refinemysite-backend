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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONELIST
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore.MilestoneListSnapshot
import com.bosch.pt.iot.smartsite.project.milestone.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.workarea.domain.toAggregateReference

object MilestoneListAvroSnapshotMapper : AbstractAvroSnapshotMapper<MilestoneListSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: MilestoneListSnapshot,
      eventType: E
  ): MilestoneListEventAvro =
      with(snapshot) {
        MilestoneListEventAvro.newBuilder()
            .setName(eventType as MilestoneListEventEnumAvro)
            .setAggregateBuilder(
                MilestoneListAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setProject(projectRef.toAggregateReference())
                    .setDate(date.toEpochMilli())
                    .setHeader(header)
                    .setWorkarea(workAreaRef?.toAggregateReference())
                    .setMilestones(milestoneRefs.map { it.toAggregateReference() }))
            .build()
      }

  override fun getAggregateType() = MILESTONELIST.value

  override fun getRootContextIdentifier(snapshot: MilestoneListSnapshot) =
      snapshot.projectRef.toUuid()
}
