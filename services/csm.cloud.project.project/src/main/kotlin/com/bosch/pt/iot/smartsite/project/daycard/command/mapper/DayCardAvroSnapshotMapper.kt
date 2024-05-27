/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.mapper

import com.bosch.pt.csm.cloud.common.command.mapper.AbstractAvroSnapshotMapper
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore.DayCardSnapshot
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import java.util.UUID

object DayCardAvroSnapshotMapper : AbstractAvroSnapshotMapper<DayCardSnapshot>() {

  override fun <E : Enum<*>> toAvroMessageWithNewVersion(
      snapshot: DayCardSnapshot,
      eventType: E
  ): DayCardEventG2Avro =
      with(snapshot) {
        DayCardEventG2Avro.newBuilder()
            .setName(eventType as DayCardEventEnumAvro)
            .setAggregateBuilder(
                DayCardAggregateG2Avro.newBuilder()
                    .setAggregateIdentifier(toAggregateIdentifierAvroWithNextVersion(snapshot))
                    .setAuditingInformation(toUpdatedAuditingInformationAvro(snapshot))
                    .setTask(taskIdentifier!!.toAggregateReference())
                    .setTitle(title)
                    .setStatus(DayCardStatusEnumAvro.valueOf(status.toString()))
                    .setManpower(manpower.setScale(2))
                    .setNotes(notes)
                    .setReason(reason?.let { DayCardReasonNotDoneEnumAvro.valueOf(it.name) }))
            .build()
      }

  override fun getAggregateType() = DAYCARD.value
  override fun getRootContextIdentifier(snapshot: DayCardSnapshot): UUID =
      snapshot.projectIdentifier.toUuid()
}
