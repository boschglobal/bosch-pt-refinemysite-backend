/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREALIST
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.project.project.toAggregateReference
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaListSnapshot
import com.bosch.pt.iot.smartsite.project.workarea.domain.toAggregateReference

object WorkAreaListAvroSnapshotMapper : AbstractAvroSnapshotMapper<WorkAreaListSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: WorkAreaListSnapshot,
      eventType: E
  ): WorkAreaListEventAvro =
      with(snapshot) {
        WorkAreaListEventAvro.newBuilder()
            .setName(eventType as WorkAreaListEventEnumAvro)
            .setAggregateBuilder(
                WorkAreaListAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setProject(projectRef.toAggregateReference())
                    .setWorkAreas(workAreaRefs.map { it.toAggregateReference() }))
            .build()
      }

  override fun getAggregateType() = WORKAREALIST.value

  override fun getRootContextIdentifier(snapshot: WorkAreaListSnapshot) =
      snapshot.projectRef.toUuid()
}
