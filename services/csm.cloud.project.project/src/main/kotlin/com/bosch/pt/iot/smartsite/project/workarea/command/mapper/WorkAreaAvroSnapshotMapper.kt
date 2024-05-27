/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore.WorkAreaSnapshot

object WorkAreaAvroSnapshotMapper : AbstractAvroSnapshotMapper<WorkAreaSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: WorkAreaSnapshot,
      eventType: E
  ): WorkAreaEventAvro =
      with(snapshot) {
        WorkAreaEventAvro.newBuilder()
            .setName(eventType as WorkAreaEventEnumAvro)
            .setAggregateBuilder(
                WorkAreaAggregateAvro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setName(name)
                    .setParent(snapshot.parentRef?.identifier?.toString()))
            .build()
      }

  override fun getAggregateType() = WORKAREA.value

  override fun getRootContextIdentifier(snapshot: WorkAreaSnapshot) = snapshot.projectRef.toUuid()
}
